package com.bbd.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 User 변경 이벤트 처리에 사용하는 설정값.

 Kafka/Redis 연동이 준비되지 않은 환경에서는 enabled=false로 두면
 Outbox 저장은 유지하면서 Publisher와 Consumer Bean만 비활성화한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bbd.user.events")
public class UserEventProperties {

    // Kafka 발행과 Redis 무효화 Consumer를 활성화할지 여부.
    private boolean enabled;

    // User 변경 이벤트를 발행하고 소비할 Kafka 원본 topic.
    private String topic = "erp.user.changed.v1";

    // 원본 topic에서 Redis Snapshot을 삭제하는 Consumer group.
    private String snapshotConsumerGroup = "user-snapshot-projector-v1";

    // DLT에서 Redis Snapshot 삭제를 복구하는 전용 Consumer group.
    private String dltRecoveryConsumerGroup = "user-snapshot-dlt-recovery-v1";

    // 원본 topic 뒤에 붙여 DLT 이름을 만들 때 사용하는 suffix.
    private String dltSuffix = ".DLT";

    // 원본 topic과 DLT에 공통으로 적용할 partition 수.
    private int topicPartitions = 1;

    // 로컬 단일 broker에서는 1, 운영 다중 broker에서는 환경에 맞게 조정한다.
    private short topicReplicas = 1;

    // bbd-security-core가 저장하는 Redis UserSnapshot key prefix.
    private String cacheKeyPrefix = "user:snapshot:";

    // Outbox Publisher가 한 번의 polling에서 가져올 최대 이벤트 수.
    private int outboxBatchSize = 100;

    // Outbox 이벤트가 FAILED로 격리되기 전까지 Kafka 발행을 시도할 최대 횟수.
    private int outboxMaxAttempts = 30;

    // Outbox polling이 끝난 뒤 다음 polling까지 기다리는 시간.
    private long publishDelayMs = 1000;

    // Kafka broker의 발행 결과를 기다릴 최대 시간.
    private long sendTimeoutMs = 5000;

    // 메인 Consumer가 실패한 record를 다시 처리하기까지 기다리는 시간.
    private long consumerRetryDelayMs = 1000;

    // 메인 Consumer가 DLT로 보내기 전에 수행할 재시도 횟수.
    private long consumerRetryMaxAttempts = 3;

    // DLT Consumer가 Redis 복구를 기다리며 다시 처리하는 간격.
    private long dltRecoveryDelayMs = 30000;
}
