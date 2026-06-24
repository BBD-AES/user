package com.bbd.user.application.event;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

/*
 User DB 변경 사실을 표현하는 application event contract.

 이 객체는 application 계층에서 만들고 두 후속 처리에 함께 사용한다.

 1. Redis Snapshot 무효화
    - SnapshotInvalidationOutboxPersistenceAdapter가
      snapshot_invalidation_outbox에 PENDING 작업으로 저장한다.
    - AFTER_COMMIT 처리기와 Scheduler가 keycloakSub 기준으로 Redis key를 삭제한다.

 2. 외부 서비스용 사용자 변경 이벤트 발행
    - UserOutboxPersistenceAdapter가 user_outbox에 PENDING 이벤트로 저장한다.
    - bbd.user.events.kafka-enabled=true인 환경에서는 UserOutboxPublisher가 Kafka에 발행한다.
    - 현재 소비 서비스가 없다면 이 경로는 비활성화하거나 향후 확장 지점으로 볼 수 있다.

 eventId:
 같은 사용자 변경에서 파생된 Kafka 발행 작업과 Redis 삭제 작업을 묶는 추적 ID.
 중복 이벤트 추적 기준으로도 사용할 수 있다.

 keycloakSub:
 Kafka message key이자 Redis UserSnapshot key를 만들 때 사용하는 기준값.

 version:
 사용자 변경 순서를 비교하거나 오래된 이벤트를 무시하는 정책으로
 확장할 때 사용할 DB 낙관적 잠금 버전.
 */
public record UserChangedEvent(
        UUID eventId,
        UserChangeType eventType,
        Instant occurredAt,
        Long userId,
        String keycloakSub,
        String employeeNumber,
        UserStatus status,
        UserRole role,
        TenancyType tenancyType,
        String tenancyName,
        Long version
) {

    /*
     DB 저장이 완료된 User 도메인 모델을 event payload로 변환한다.
     JPA @Version이 증가한 뒤의 User를 받아야 실제 DB version과 event version이 일치한다.
     */
    public static UserChangedEvent from(User user, UserChangeType eventType) {
        return new UserChangedEvent(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                user.id(),
                user.keycloakSub(),
                user.employeeNumber(),
                user.status(),
                user.role(),
                user.tenancyType(),
                user.tenancyName(),
                user.version()
        );
    }
}
