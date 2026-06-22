package com.bbd.user.adapter.in.event;

import com.bbd.user.adapter.out.snapshot.SnapshotInvalidationOutboxProcessor;
import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/*
 AFTER_COMMIT listener가 snapshot invalidation outbox processor에 위임하는지 검증한다.
 */
class UserSnapshotImmediateCacheInvalidatorTest {

    @Test
    void delegatesAfterCommitInvalidationToProcessor() {
        SnapshotInvalidationOutboxProcessor processor =
                mock(SnapshotInvalidationOutboxProcessor.class);
        UserSnapshotImmediateCacheInvalidator invalidator =
                new UserSnapshotImmediateCacheInvalidator(processor);
        UserChangedEvent event = event("target-sub");

        invalidator.invalidate(event);

        verify(processor).invalidateAfterCommit(event);
    }

    private static UserChangedEvent event(String keycloakSub) {
        return new UserChangedEvent(
                UUID.randomUUID(),
                UserChangeType.USER_AUTHORIZATION_CHANGED,
                Instant.now(),
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
