package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

/*
 User 도메인을 외부 adapter에 노출하지 않기 위한 application 계층의 공통 사용자 결과 모델.
 */
public record UserResult(
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

    public static UserResult from(User user) {
        return new UserResult(
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
