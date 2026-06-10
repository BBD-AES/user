package com.bbd.user.adapter.in.web.response;

import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

/*
 공통 인가 프레임워크가 Redis miss 시 호출하는
 UserSnapshot API의 응답 DTO.

 이 객체는 web adapter 계층의 response 모델이다.
 application 계층의 UserSnapshotResult를 HTTP 응답 형태로 변환한다.
 */
public record UserSnapshotResponse(
        Long userId,
        String keycloakSub,
        String employeeNumber,
        String username,
        String displayName,
        String email,
        String position,
        UserStatus status,
        UserRole role,
        TenancyType tenancyType,
        Long tenancyId,
        String tenancyName,
        Long version
) {

    public static UserSnapshotResponse from(UserSnapshotResult result) {
        return new UserSnapshotResponse(
                result.userId(),
                result.keycloakSub(),
                result.employeeNumber(),
                result.username(),
                result.displayName(),
                result.email(),
                result.position(),
                result.status(),
                result.role(),
                result.tenancyType(),
                result.tenancyId(),
                result.tenancyName(),
                result.version()
        );
    }
}