package com.bbd.user.adapter.out.snapshot;

import com.bbd.user.application.event.UserChangedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/*
 snapshot_invalidation_outbox 테이블과 매핑되는 persistence Entity.

 Redis Snapshot 삭제는 User 변경 transaction의 부가 작업이다.
 삭제해야 할 keycloakSub를 먼저 PENDING으로 남기고,
 commit 이후 즉시 삭제 또는 scheduler 재시도로 DONE/FAILED 상태를 결정한다.
 */
@Getter
@Entity
@Table(name = "snapshot_invalidation_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnapshotInvalidationOutboxJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "keycloak_sub", nullable = false, length = 100)
    private String keycloakSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SnapshotInvalidationOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public static SnapshotInvalidationOutboxJpaEntity pending(UserChangedEvent event) {
        SnapshotInvalidationOutboxJpaEntity entity = new SnapshotInvalidationOutboxJpaEntity();
        entity.eventId = event.eventId();
        entity.aggregateId = event.userId();
        entity.eventType = event.eventType().name();
        entity.keycloakSub = event.keycloakSub();
        entity.status = SnapshotInvalidationOutboxStatus.PENDING;
        entity.attempts = 0;
        entity.occurredAt = event.occurredAt();
        return entity;
    }

    public void markDone(Instant invalidatedAt) {
        this.status = SnapshotInvalidationOutboxStatus.DONE;
        this.invalidatedAt = invalidatedAt;
        this.lastError = null;
    }

    public void markFailedAttempt(Throwable error, int maxAttempts) {
        this.attempts++;
        String message = error.getMessage();
        this.lastError = message == null
                ? error.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 1000));
        if (this.attempts >= Math.max(1, maxAttempts)) {
            this.status = SnapshotInvalidationOutboxStatus.FAILED;
        }
    }
}
