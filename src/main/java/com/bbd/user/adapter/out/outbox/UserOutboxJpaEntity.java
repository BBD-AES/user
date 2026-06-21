package com.bbd.user.adapter.out.outbox;

import com.bbd.user.application.event.UserChangedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/*
 user_outbox 테이블과 매핑되는 persistence Entity.

 User Service는 User DB 변경 직후 Kafka에 직접 발행하지 않는다.
 User 변경과 같은 DB 트랜잭션에 이 Entity를 PENDING으로 저장하고,
 별도의 UserOutboxPublisher가 commit된 row만 Kafka로 발행한다.

 이 구조로 다음 문제를 막는다.

 - User DB는 변경됐지만 Kafka 발행이 실패해서 변경 이벤트가 사라지는 문제
 - Kafka 장애 때문에 사용자 변경 트랜잭션 전체가 장시간 대기하는 문제

 Kafka 발행은 at-least-once 방식이므로 중복될 수 있다.
 Consumer는 eventId 또는 멱등 연산을 기준으로 중복을 허용해야 한다.
 */
@Getter
@Entity
@Table(name = "user_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOutboxJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    /*
     application event를 최초 PENDING Outbox Entity로 변환한다.

     eventKey에는 keycloakSub를 저장한다.
     Kafka message key로 사용해서 동일 사용자의 이벤트가 같은 partition으로 가도록 한다.
     */
    public static UserOutboxJpaEntity pending(UserChangedEvent event, String payload) {
        UserOutboxJpaEntity entity = new UserOutboxJpaEntity();
        entity.eventId = event.eventId();
        entity.aggregateType = "USER";
        entity.aggregateId = event.userId();
        entity.eventType = event.eventType().name();
        entity.eventKey = event.keycloakSub();
        entity.payload = payload;
        entity.status = UserOutboxStatus.PENDING;
        entity.attempts = 0;
        entity.occurredAt = event.occurredAt();
        return entity;
    }

    // Kafka broker가 send를 성공으로 확인한 뒤 발행 완료 시각을 기록한다.
    public void markPublished(Instant publishedAt) {
        this.status = UserOutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    /*
     발행 실패 시 재시도 횟수와 마지막 오류를 기록한다.
     재시도 상한에 도달하면 FAILED로 격리해 이후 PENDING polling 대상에서 제외한다.
     오류 메시지는 DB column 길이에 맞춰 최대 1000자로 자른다.
     */
    public void markFailed(Throwable error, int maxAttempts) {
        this.attempts++;
        String message = error.getMessage();
        this.lastError = message == null
                ? error.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 1000));
        if (this.attempts >= Math.max(1, maxAttempts)) {
            this.status = UserOutboxStatus.FAILED;
        }
    }
}
