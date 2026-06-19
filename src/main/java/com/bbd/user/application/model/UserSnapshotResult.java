package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

/*
 UserSnapshot 조회 유스케이스의 결과 모델.

 이 객체는 외부 API 응답 DTO가 아니라 application 계층의 결과 모델이다.

 최종적으로 adapter.in.web 계층에서 UserSnapshotResponse로 변환되어
 공통 인가 프레임워크가 호출할 Snapshot API 응답으로 내려간다.
 */
public record UserSnapshotResult(
        Long userId,
        String keycloakSub,
        String employeeNumber,
        String displayName,
        String email,
        String position,
        UserStatus status,
        UserRole role,
        TenancyType tenancyType,
        String tenancyName,
        Long version
) {

    public static UserSnapshotResult from(User user) {
        return new UserSnapshotResult(
                user.id(),
                user.keycloakSub(),
                user.employeeNumber(),
                user.displayName(),
                user.email(),
                user.position(),
                user.status(),
                user.role(),
                user.tenancyType(),
                user.tenancyName(),
                user.version()
        );
    }
}