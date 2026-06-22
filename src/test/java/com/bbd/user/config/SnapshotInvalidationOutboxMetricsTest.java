package com.bbd.user.config;

import com.bbd.user.adapter.out.snapshot.SnapshotInvalidationOutboxJpaRepository;
import com.bbd.user.adapter.out.snapshot.SnapshotInvalidationOutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SnapshotInvalidationOutboxMetricsTest {

    @Test
    void exposesPendingAndFailedSnapshotInvalidationGauges() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SnapshotInvalidationOutboxJpaRepository repository =
                mock(SnapshotInvalidationOutboxJpaRepository.class);

        when(repository.countByStatus(SnapshotInvalidationOutboxStatus.PENDING)).thenReturn(4L);
        when(repository.countByStatus(SnapshotInvalidationOutboxStatus.FAILED)).thenReturn(1L);

        new SnapshotInvalidationOutboxMetrics(meterRegistry, repository);

        assertEquals(
                4.0,
                meterRegistry.get("bbd.user.snapshot.invalidation.pending").gauge().value()
        );
        assertEquals(
                1.0,
                meterRegistry.get("bbd.user.snapshot.invalidation.failed").gauge().value()
        );
    }
}
