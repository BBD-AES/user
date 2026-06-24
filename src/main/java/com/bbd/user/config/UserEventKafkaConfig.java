package com.bbd.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/*
 USER_CHANGED Kafka 발행에 필요한 topic 설정.

 User Service는 변경 이벤트를 발행만 한다.
 Redis Snapshot 삭제 복구는 snapshot_invalidation_outbox가 담당하므로
 이 서비스가 자기 Kafka topic을 소비하거나 DLT를 생성하지 않는다.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "kafka-enabled",
        havingValue = "true"
)
public class UserEventKafkaConfig {

    /*
     원본 topic이 없으면 KafkaAdmin이 생성한다.
     소비 서비스의 DLT topic은 해당 소비 서비스가 소유한다.
     */
    @Bean
    NewTopic userChangedTopic(UserEventProperties properties) {
        return TopicBuilder.name(properties.getTopic())
                .partitions(properties.getTopicPartitions())
                .replicas(properties.getTopicReplicas())
                .build();
    }
}
