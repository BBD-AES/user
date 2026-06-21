package com.bbd.user.adapter.out.kafka;

import com.bbd.user.adapter.out.outbox.UserOutboxJpaEntity;
import com.bbd.user.adapter.out.outbox.UserOutboxJpaRepository;
import com.bbd.user.adapter.out.outbox.UserOutboxStatus;
import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.port.out.PublishUserEventPort;
import com.bbd.user.config.UserEventProperties;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserOutboxPublisherTest {

    @Test
    void successfulPublishMarksEventsPublished() throws Exception {
        UserOutboxJpaRepository userOutboxJpaRepository = mock(UserOutboxJpaRepository.class);
        PublishUserEventPort publishUserEventPort = mock(PublishUserEventPort.class);
        UserEventProperties properties = new UserEventProperties();
        properties.setOutboxBatchSize(2);
        UserOutboxJpaEntity first = pendingEvent("first-sub");
        UserOutboxJpaEntity second = pendingEvent("second-sub");

        when(userOutboxJpaRepository.findPendingForPublish(2)).thenReturn(List.of(first, second));

        new UserOutboxPublisher(userOutboxJpaRepository, publishUserEventPort, properties)
                .publishPendingEvents();

        assertEquals(UserOutboxStatus.PUBLISHED, first.getStatus());
        assertEquals(0, first.getAttempts());
        assertNotNull(first.getPublishedAt());
        assertNull(first.getLastError());
        assertEquals(UserOutboxStatus.PUBLISHED, second.getStatus());
        assertEquals(0, second.getAttempts());
        assertNotNull(second.getPublishedAt());
        verify(publishUserEventPort).publish(eq("first-sub"), eq("{}"));
        verify(publishUserEventPort).publish(eq("second-sub"), eq("{}"));
    }

    @Test
    void failedPublishRecordsAttemptAndContinuesBatch() throws Exception {
        UserOutboxJpaRepository userOutboxJpaRepository = mock(UserOutboxJpaRepository.class);
        PublishUserEventPort publishUserEventPort = mock(PublishUserEventPort.class);
        UserEventProperties properties = new UserEventProperties();
        properties.setOutboxBatchSize(2);
        properties.setOutboxMaxAttempts(3);
        UserOutboxJpaEntity first = pendingEvent("first-sub");
        UserOutboxJpaEntity second = pendingEvent("second-sub");

        when(userOutboxJpaRepository.findPendingForPublish(2)).thenReturn(List.of(first, second));
        doThrow(new TimeoutException("broker timeout"))
                .when(publishUserEventPort)
                .publish(eq("first-sub"), eq("{}"));

        new UserOutboxPublisher(userOutboxJpaRepository, publishUserEventPort, properties)
                .publishPendingEvents();

        assertEquals(UserOutboxStatus.PENDING, first.getStatus());
        assertEquals(1, first.getAttempts());
        assertEquals("broker timeout", first.getLastError());
        assertEquals(UserOutboxStatus.PUBLISHED, second.getStatus());
        assertEquals(0, second.getAttempts());
        verify(publishUserEventPort, times(2)).publish(anyString(), anyString());
    }

    @Test
    void failedPublishMovesEventToFailedAtMaxAttempts() throws Exception {
        UserOutboxJpaRepository userOutboxJpaRepository = mock(UserOutboxJpaRepository.class);
        PublishUserEventPort publishUserEventPort = mock(PublishUserEventPort.class);
        UserEventProperties properties = new UserEventProperties();
        properties.setOutboxBatchSize(1);
        properties.setOutboxMaxAttempts(1);
        UserOutboxJpaEntity event = pendingEvent("failed-sub");

        when(userOutboxJpaRepository.findPendingForPublish(1)).thenReturn(List.of(event));
        doThrow(new TimeoutException("permanent failure"))
                .when(publishUserEventPort)
                .publish(eq("failed-sub"), eq("{}"));

        new UserOutboxPublisher(userOutboxJpaRepository, publishUserEventPort, properties)
                .publishPendingEvents();

        assertEquals(UserOutboxStatus.FAILED, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertEquals("permanent failure", event.getLastError());
    }

    @Test
    void interruptedSendDoesNotMarkFailedAndStopsCurrentBatch() throws Exception {
        UserOutboxJpaRepository userOutboxJpaRepository = mock(UserOutboxJpaRepository.class);
        PublishUserEventPort publishUserEventPort = mock(PublishUserEventPort.class);
        UserEventProperties properties = new UserEventProperties();
        properties.setOutboxBatchSize(2);
        properties.setSendTimeoutMs(1000);
        UserOutboxJpaEntity first = pendingEvent("first-sub");
        UserOutboxJpaEntity second = pendingEvent("second-sub");

        when(userOutboxJpaRepository.findPendingForPublish(2)).thenReturn(List.of(first, second));
        doThrow(new InterruptedException("shutdown"))
                .when(publishUserEventPort)
                .publish(anyString(), anyString());

        Thread.interrupted();
        try {
            new UserOutboxPublisher(userOutboxJpaRepository, publishUserEventPort, properties)
                    .publishPendingEvents();

            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(UserOutboxStatus.PENDING, first.getStatus());
            assertEquals(0, first.getAttempts());
            assertEquals(UserOutboxStatus.PENDING, second.getStatus());
            assertEquals(0, second.getAttempts());
            verify(publishUserEventPort, times(1)).publish(anyString(), anyString());
        } finally {
            Thread.interrupted();
        }
    }

    private UserOutboxJpaEntity pendingEvent(String keycloakSub) {
        return UserOutboxJpaEntity.pending(
                new UserChangedEvent(
                        UUID.randomUUID(),
                        UserChangeType.USER_CREATED,
                        Instant.parse("2026-06-21T00:00:00Z"),
                        1L,
                        keycloakSub,
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
