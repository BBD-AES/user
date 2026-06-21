package com.bbd.user.adapter.out.outbox;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserOutboxJpaEntityTest {

    @Test
    void keepsPendingBeforeMaxAttempts() {
        UserOutboxJpaEntity event = pendingEvent();

        event.markFailed(new IllegalStateException("Kafka 발행 실패"), 3);

        assertEquals(UserOutboxStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertEquals("Kafka 발행 실패", event.getLastError());
    }

    @Test
    void marksFailedWhenMaxAttemptsIsReached() {
        UserOutboxJpaEntity event = pendingEvent();

        event.markFailed(new IllegalStateException("첫 번째 실패"), 3);
        event.markFailed(new IllegalStateException("두 번째 실패"), 3);
        event.markFailed(new IllegalStateException("세 번째 실패"), 3);

        assertEquals(UserOutboxStatus.FAILED, event.getStatus());
        assertEquals(3, event.getAttempts());
        assertEquals("세 번째 실패", event.getLastError());
    }

    @Test
    void nonPositiveMaxAttemptsFailsOnFirstFailure() {
        UserOutboxJpaEntity event = pendingEvent();

        event.markFailed(new IllegalStateException("즉시 실패"), 0);

        assertEquals(UserOutboxStatus.FAILED, event.getStatus());
        assertEquals(1, event.getAttempts());
    }

    private UserOutboxJpaEntity pendingEvent() {
        return UserOutboxJpaEntity.pending(
                new UserChangedEvent(
                        UUID.randomUUID(),
                        UserChangeType.USER_CREATED,
                        Instant.parse("2026-06-21T00:00:00Z"),
                        1L,
                        "keycloak-sub",
                        "EMP-001",
                        UserStatus.PENDING,
                        UserRole.BRANCH_STAFF,
                        TenancyType.BRANCH,
                        "강남 지점",
                        1L
                ),
                "{}"
        );
    }
}
