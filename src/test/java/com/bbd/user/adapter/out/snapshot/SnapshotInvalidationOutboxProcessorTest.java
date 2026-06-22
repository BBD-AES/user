package com.bbd.user.adapter.out.snapshot;

import com.bbd.user.adapter.out.redis.UserSnapshotCacheEvictor;
import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventProperties;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SnapshotInvalidationOutboxProcessorTest {

    @Test
    void afterCommitSuccessMarksOutboxDone() {
        SnapshotInvalidationOutboxJpaRepository repository =
                mock(SnapshotInvalidationOutboxJpaRepository.class);
        UserSnapshotCacheEvictor cacheEvictor = mock(UserSnapshotCacheEvictor.class);
        UserEventProperties properties = new UserEventProperties();
        UserChangedEvent event = event("target-sub");
        SnapshotInvalidationOutboxJpaEntity outbox =
                SnapshotInvalidationOutboxJpaEntity.pending(event);

        when(repository.findById(event.eventId())).thenReturn(Optional.of(outbox));
        when(cacheEvictor.evict(eq("target-sub"), eq("after_commit")))
                .thenReturn("user:snapshot:target-sub");

        processor(repository, cacheEvictor, properties).invalidateAfterCommit(event);

        assertEquals(SnapshotInvalidationOutboxStatus.DONE, outbox.getStatus());
        assertEquals(0, outbox.getAttempts());
        assertNotNull(outbox.getInvalidatedAt());
        assertNull(outbox.getLastError());
    }

    @Test
    void afterCommitFailureRecordsAttemptAndCanMoveToFailed() {
        SnapshotInvalidationOutboxJpaRepository repository =
                mock(SnapshotInvalidationOutboxJpaRepository.class);
        UserSnapshotCacheEvictor cacheEvictor = mock(UserSnapshotCacheEvictor.class);
        UserEventProperties properties = new UserEventProperties();
        properties.setSnapshotInvalidationMaxAttempts(1);
        UserChangedEvent event = event("target-sub");
        SnapshotInvalidationOutboxJpaEntity outbox =
                SnapshotInvalidationOutboxJpaEntity.pending(event);

        when(repository.findById(event.eventId())).thenReturn(Optional.of(outbox));
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(cacheEvictor)
                .evict(eq("target-sub"), eq("after_commit"));

        processor(repository, cacheEvictor, properties).invalidateAfterCommit(event);

        assertEquals(SnapshotInvalidationOutboxStatus.FAILED, outbox.getStatus());
        assertEquals(1, outbox.getAttempts());
        assertEquals("Redis unavailable", outbox.getLastError());
        assertNull(outbox.getInvalidatedAt());
    }

    @Test
    void schedulerRetriesPendingRowsThroughSnapshotOutboxSource() {
        SnapshotInvalidationOutboxJpaRepository repository =
                mock(SnapshotInvalidationOutboxJpaRepository.class);
        UserSnapshotCacheEvictor cacheEvictor = mock(UserSnapshotCacheEvictor.class);
        UserEventProperties properties = new UserEventProperties();
        properties.setSnapshotInvalidationBatchSize(2);
        SnapshotInvalidationOutboxJpaEntity first =
                SnapshotInvalidationOutboxJpaEntity.pending(event("first-sub"));
        SnapshotInvalidationOutboxJpaEntity second =
                SnapshotInvalidationOutboxJpaEntity.pending(event("second-sub"));

        when(repository.findPendingForRetry(2)).thenReturn(List.of(first, second));
        when(cacheEvictor.evict(eq("first-sub"), eq("snapshot_outbox")))
                .thenReturn("user:snapshot:first-sub");
        when(cacheEvictor.evict(eq("second-sub"), eq("snapshot_outbox")))
                .thenReturn("user:snapshot:second-sub");

        processor(repository, cacheEvictor, properties).retryPendingInvalidations();

        assertEquals(SnapshotInvalidationOutboxStatus.DONE, first.getStatus());
        assertEquals(SnapshotInvalidationOutboxStatus.DONE, second.getStatus());
        verify(cacheEvictor).evict("first-sub", "snapshot_outbox");
        verify(cacheEvictor).evict("second-sub", "snapshot_outbox");
    }

    private SnapshotInvalidationOutboxProcessor processor(
            SnapshotInvalidationOutboxJpaRepository repository,
            UserSnapshotCacheEvictor cacheEvictor,
            UserEventProperties properties
    ) {
        return new SnapshotInvalidationOutboxProcessor(repository, cacheEvictor, properties);
    }

    private static UserChangedEvent event(String keycloakSub) {
        return new UserChangedEvent(
                UUID.randomUUID(),
                UserChangeType.USER_AUTHORIZATION_CHANGED,
                Instant.parse("2026-06-21T00:00:00Z"),
                1L,
                keycloakSub,
                "EMP-1",
                UserStatus.ACTIVE,
                UserRole.HQ_STAFF,
                TenancyType.HQ,
                "본사",
                2L
        );
    }
}
