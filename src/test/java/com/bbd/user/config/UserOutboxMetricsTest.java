package com.bbd.user.config;

import com.bbd.user.adapter.out.outbox.UserOutboxJpaRepository;
import com.bbd.user.adapter.out.outbox.UserOutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserOutboxMetricsTest {

    @Test
    void exposesPendingAndFailedOutboxGauges() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        UserOutboxJpaRepository userOutboxJpaRepository = mock(UserOutboxJpaRepository.class);

        when(userOutboxJpaRepository.countByStatus(UserOutboxStatus.PENDING)).thenReturn(5L);
        when(userOutboxJpaRepository.countByStatus(UserOutboxStatus.FAILED)).thenReturn(2L);

        new UserOutboxMetrics(meterRegistry, userOutboxJpaRepository);

        assertEquals(5.0, meterRegistry.get("bbd.user.outbox.pending").gauge().value());
        assertEquals(2.0, meterRegistry.get("bbd.user.outbox.failed").gauge().value());
    }
}
