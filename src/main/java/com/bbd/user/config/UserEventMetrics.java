package com.bbd.user.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/*
 User 변경 이벤트 처리 결과를 Actuator/Prometheus 지표로 노출한다.

 Redis 삭제 결과는 source와 result tag로 구분한다.
 */
@Component
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "snapshot-invalidation-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UserEventMetrics {

    private final MeterRegistry meterRegistry;

    public UserEventMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Redis 삭제 경로와 처리 결과를 tag로 구분해서 기록한다.
    public void recordSnapshotEviction(String source, String result) {
        meterRegistry.counter(
                "bbd.user.snapshot.eviction",
                "source", source,
                "result", result
        ).increment();
    }
}
