package com.bbd.user.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/*
 USER_CHANGED Kafka Consumer의 공통 실패 처리 설정.

 Redis 장애나 잘못된 event payload 때문에 Consumer가 실패하면
 같은 record를 1초 간격으로 최대 3번 재시도한다.

 재시도 후에도 실패한 record는 원본 topic 이름 뒤에 .DLT를 붙인 topic으로 보낸다.

 예:
 erp.user.changed.v1
 -> erp.user.changed.v1.DLT

 DLT는 운영자가 원인을 수정한 뒤 별도의 재처리 절차로 복구하기 위한 저장소다.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "enabled",
        havingValue = "true"
)
public class UserEventKafkaConfig {

    /*
     record를 DLT로 보낼 때 원본 partition 번호를 유지한다.
     따라서 원본 topic과 DLT는 같은 partition 수로 생성해야 한다.
     */
    @Bean
    CommonErrorHandler userChangedEventErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(record.topic() + ".DLT", record.partition())
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
