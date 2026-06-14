package com.bbd.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

/*
 USER_CHANGED Kafka Consumer의 공통 실패 처리 설정.

 Redis 장애나 잘못된 event payload 때문에 Consumer가 실패하면
 같은 record를 1초 간격으로 최대 3번 재시도한다.

 재시도 후에도 실패한 record는 원본 topic 이름 뒤에 .DLT를 붙인 topic으로 보낸다.

 예:
 erp.user.changed.v1
 -> erp.user.changed.v1.DLT

 DLT는 메인 Consumer 흐름과 분리해서 Redis 복구를 기다리는 저장소다.
 별도 DLT Consumer가 Redis가 정상화될 때까지 낮은 빈도로 다시 처리한다.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "enabled",
        havingValue = "true"
)
public class UserEventKafkaConfig {

    /*
     원본 topic과 DLT를 같은 partition/replica 설정으로 생성한다.
     이미 존재하는 topic은 그대로 사용하며 없는 topic만 KafkaAdmin이 생성한다.
     */
    @Bean
    NewTopic userChangedTopic(UserEventProperties properties) {
        return TopicBuilder.name(properties.getTopic())
                .partitions(properties.getTopicPartitions())
                .replicas(properties.getTopicReplicas())
                .build();
    }

    @Bean
    NewTopic userChangedDltTopic(UserEventProperties properties) {
        return TopicBuilder.name(properties.getTopic() + properties.getDltSuffix())
                .partitions(properties.getTopicPartitions())
                .replicas(properties.getTopicReplicas())
                .build();
    }

    /*
     record를 DLT로 보낼 때 원본 partition 번호를 유지한다.
     따라서 원본 topic과 DLT는 같은 partition 수로 생성해야 한다.
     */
    @Bean
    CommonErrorHandler userChangedEventErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            UserEventProperties properties,
            UserEventMetrics metrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(
                                record.topic() + properties.getDltSuffix(),
                                record.partition()
                        )
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(
                        properties.getConsumerRetryDelayMs(),
                        properties.getConsumerRetryMaxAttempts()
                )
        );

        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(
                    ConsumerRecord<?, ?> record,
                    Exception exception,
                    int deliveryAttempt
            ) {
                if (deliveryAttempt > 1) {
                    metrics.recordConsumerRetry();
                }
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
                metrics.recordDltPublished("success");
            }

            @Override
            public void recoveryFailed(
                    ConsumerRecord<?, ?> record,
                    Exception original,
                    Exception failure
            ) {
                metrics.recordDltPublished("failure");
            }
        });

        return errorHandler;
    }

    /*
     DLT 전용 Consumer는 Redis가 복구될 때까지 같은 record를 계속 재시도한다.
     메인 topic과 별도 factory를 사용하므로 DLT 대기가 정상 이벤트 처리를 막지 않는다.
     */
    @Bean(name = "userEventDltKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<Object, Object>
    userEventDltKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            UserEventProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(
                        new FixedBackOff(
                                properties.getDltRecoveryDelayMs(),
                                FixedBackOff.UNLIMITED_ATTEMPTS
                        )
                )
        );
        return factory;
    }
}
