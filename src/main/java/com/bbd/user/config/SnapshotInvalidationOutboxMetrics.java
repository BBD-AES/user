package com.bbd.user.config;

import com.bbd.user.adapter.out.snapshot.SnapshotInvalidationOutboxJpaRepository;
import com.bbd.user.adapter.out.snapshot.SnapshotInvalidationOutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/*
 Redis Snapshot 무효화 outbox의 PENDING/FAILED 개수를 노출한다.

 FAILED row는 Kafka DLT가 아니라 User Service DB 안의 운영 격리 상태다.
 값이 0보다 크면 Redis 장애나 cache key 설정 오류를 확인해야 한다.
 */
@Component
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "snapshot-invalidation-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SnapshotInvalidationOutboxMetrics {

    public SnapshotInvalidationOutboxMetrics(
            MeterRegistry meterRegistry,
            SnapshotInvalidationOutboxJpaRepository repository
    ) {
        Gauge.builder(
                        "bbd.user.snapshot.invalidation.pending",
                        repository,
                        source -> source.countByStatus(SnapshotInvalidationOutboxStatus.PENDING)
                )
                .description("Redis 삭제 전 PENDING Snapshot Invalidation Outbox 개수")
                .register(meterRegistry);

        Gauge.builder(
                        "bbd.user.snapshot.invalidation.failed",
                        repository,
                        source -> source.countByStatus(SnapshotInvalidationOutboxStatus.FAILED)
                )
                .description("Redis 삭제 재시도 상한에 도달한 FAILED Snapshot Invalidation Outbox 개수")
                .register(meterRegistry);
    }
}
