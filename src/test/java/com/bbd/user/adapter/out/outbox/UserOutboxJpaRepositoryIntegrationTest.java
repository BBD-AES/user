package com.bbd.user.adapter.out.outbox;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:outbox-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Transactional
class UserOutboxJpaRepositoryIntegrationTest {

    @Autowired
    private UserOutboxJpaRepository userOutboxJpaRepository;

    @Test
    void findsOnlyPendingEventsInOccurredOrderWithinBatchSize() {
        UserOutboxJpaEntity firstPending =
                pendingEvent("first-sub", "2026-06-21T00:00:01Z");
        UserOutboxJpaEntity published =
                pendingEvent("published-sub", "2026-06-21T00:00:02Z");
        published.markPublished(Instant.parse("2026-06-21T00:01:00Z"));
        UserOutboxJpaEntity failed =
                pendingEvent("failed-sub", "2026-06-21T00:00:03Z");
        failed.markFailed(new IllegalStateException("permanent failure"), 1);
        UserOutboxJpaEntity secondPending =
                pendingEvent("second-sub", "2026-06-21T00:00:04Z");
        UserOutboxJpaEntity thirdPending =
                pendingEvent("third-sub", "2026-06-21T00:00:05Z");

        userOutboxJpaRepository.saveAllAndFlush(List.of(
                thirdPending,
                failed,
                secondPending,
                published,
                firstPending
        ));

        List<UserOutboxJpaEntity> events = userOutboxJpaRepository.findPendingForPublish(2);

        assertEquals(2, events.size());
        assertEquals(firstPending.getEventId(), events.get(0).getEventId());
        assertEquals(secondPending.getEventId(), events.get(1).getEventId());
    }

    private UserOutboxJpaEntity pendingEvent(String keycloakSub, String occurredAt) {
        return UserOutboxJpaEntity.pending(
                new UserChangedEvent(
                        UUID.randomUUID(),
                        UserChangeType.USER_AUTHORIZATION_CHANGED,
                        Instant.parse(occurredAt),
                        1L,
                        keycloakSub,
                        "EMP-001",
                        UserStatus.ACTIVE,
                        UserRole.BRANCH_STAFF,
                        TenancyType.BRANCH,
                        "Gangnam Branch",
                        1L
                ),
                "{}"
        );
    }
}
