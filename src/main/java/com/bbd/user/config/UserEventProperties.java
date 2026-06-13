package com.bbd.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 User 변경 이벤트 처리에 사용하는 설정값.

 application-local.yml 또는 application-public.yml의
 bbd.user.events.* 값을 이 객체로 받는다.

 Kafka와 Redis가 준비되지 않은 환경에서는 enabled=false로 두면
 Outbox 저장은 계속 수행하지만 Publisher와 Consumer Bean은 생성하지 않는다.
 이후 enabled=true로 다시 배포하면 쌓여 있던 PENDING Outbox가 발행된다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bbd.user.events")
public class UserEventProperties {

    // Kafka 발행과 Redis 무효화 Consumer를 활성화할지 여부.
    private boolean enabled;

    // User 변경 이벤트를 발행하고 소비할 Kafka topic.
    private String topic = "erp.user.changed.v1";

    // Redis Snapshot 무효화 Consumer의 Kafka consumer group.
    private String snapshotConsumerGroup = "user-snapshot-projector-v1";

    // bbd-security-core가 저장하는 Redis UserSnapshot key prefix.
    private String cacheKeyPrefix = "user:snapshot:";

    // Outbox Publisher가 한 번의 polling에서 가져올 최대 이벤트 수.
    private int outboxBatchSize = 100;

    // Outbox polling이 끝난 뒤 다음 polling까지 기다리는 시간.
    private long publishDelayMs = 1000;

    // Kafka broker 응답을 기다릴 최대 시간.
    private long sendTimeoutMs = 5000;
}
