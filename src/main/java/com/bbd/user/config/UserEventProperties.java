package com.bbd.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 User 변경 이벤트 처리에 사용하는 설정값.

 Kafka/Redis 연동이 준비되지 않은 환경에서는 enabled=false로 두면
 Outbox 저장은 유지하면서 Publisher, Redis 즉시 삭제, Retry Scheduler만 비활성화한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bbd.user.events")
public class UserEventProperties {

    // Kafka 발행과 Redis 무효화 처리를 활성화할지 여부.
    private boolean enabled;

    // User 변경 이벤트를 발행할 Kafka topic.
    private String topic = "erp.user.changed.v1";

    // 원본 topic에 적용할 partition 수.
    private int topicPartitions = 1;

    // 로컬 단일 broker에서는 1, 운영 다중 broker에서는 환경에 맞게 조정한다.
    private short topicReplicas = 1;

    // bbd-security-core가 저장하는 Redis UserSnapshot key prefix.
    private String cacheKeyPrefix = "user:snapshot:";

    // Outbox Publisher가 한 번의 polling에서 가져올 최대 이벤트 수.
    private int outboxBatchSize = 100;

    // Outbox 이벤트가 FAILED로 격리되기 전까지 Kafka 발행을 시도할 최대 횟수.
    private int outboxMaxAttempts = 30;

    // Redis Snapshot 무효화 outbox를 한 번의 polling에서 가져올 최대 작업 수.
    private int snapshotInvalidationBatchSize = 100;

    // Redis 삭제 작업이 FAILED로 격리되기 전까지 시도할 최대 횟수.
    private int snapshotInvalidationMaxAttempts = 30;

    // Redis Snapshot 무효화 retry polling 간격.
    private long snapshotInvalidationRetryDelayMs = 5000;

    // Outbox polling이 끝난 뒤 다음 polling까지 기다리는 시간.
    private long publishDelayMs = 1000;

    // Kafka broker의 발행 결과를 기다릴 최대 시간.
    private long sendTimeoutMs = 5000;
}
