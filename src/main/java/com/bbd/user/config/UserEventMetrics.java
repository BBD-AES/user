package com.bbd.user.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/*
 User 변경 이벤트 처리 결과를 Actuator/Prometheus 지표로 노출한다.

 Redis 삭제 결과는 source와 result tag로 구분하고,
 DLT 복구 시도와 완료 횟수를 각각 기록한다.
 */
@Component
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
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

    // 메인 Consumer가 최초 처리를 제외하고 실제로 다시 시도한 횟수다.
    public void recordConsumerRetry() {
        meterRegistry.counter("bbd.user.event.consumer.retry").increment();
    }

    // 재시도 소진 record를 DLT에 발행한 성공/실패 결과다.
    public void recordDltPublished(String result) {
        meterRegistry.counter(
                "bbd.user.event.dlt.published",
                "result", result
        ).increment();
    }

    // DLT Consumer가 record 처리를 시작한 횟수다.
    public void recordDltAttempt() {
        meterRegistry.counter("bbd.user.event.dlt.attempt").increment();
    }

    // DLT record의 Redis Snapshot 삭제까지 완료한 횟수다.
    public void recordDltRecovered() {
        meterRegistry.counter("bbd.user.event.dlt.recovered").increment();
    }
}
