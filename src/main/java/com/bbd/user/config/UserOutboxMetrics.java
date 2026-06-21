package com.bbd.user.config;

import com.bbd.user.adapter.out.outbox.UserOutboxJpaRepository;
import com.bbd.user.adapter.out.outbox.UserOutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/*
 Kafka로 아직 발행되지 않은 PENDING User Outbox 개수를 노출한다.

 값이 계속 증가하거나 일정 시간 이상 0으로 돌아오지 않으면
 Kafka 연결 장애 또는 Outbox Publisher 장애를 의심할 수 있다.
 */
@Component
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class UserOutboxMetrics {

    public UserOutboxMetrics(
            MeterRegistry meterRegistry,
            UserOutboxJpaRepository userOutboxJpaRepository
    ) {
        Gauge.builder(
                        "bbd.user.outbox.pending",
                        userOutboxJpaRepository,
                        repository -> repository.countByStatus(UserOutboxStatus.PENDING)
                )
                .description("Kafka 발행 전 PENDING User Outbox 개수")
                .register(meterRegistry);

        Gauge.builder(
                        "bbd.user.outbox.failed",
                        userOutboxJpaRepository,
                        repository -> repository.countByStatus(UserOutboxStatus.FAILED)
                )
                .description("Kafka 발행 재시도 상한에 도달한 FAILED User Outbox 개수")
                .register(meterRegistry);
    }
}
