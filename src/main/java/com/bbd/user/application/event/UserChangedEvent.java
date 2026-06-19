package com.bbd.user.application.event;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

/*
 User DB 변경 사실을 Kafka로 전달하기 위한 event contract.

 이 객체는 application 계층에서 만들고,
 UserOutboxPersistenceAdapter가 JSON payload로 직렬화해서 user_outbox에 저장한다.

 eventId:
 동일 이벤트의 중복 전달 여부를 추적할 수 있는 고유값.

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
