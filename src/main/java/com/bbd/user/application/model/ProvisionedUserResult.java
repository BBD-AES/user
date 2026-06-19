package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

/*
 SCIM adapter가 User 도메인을 직접 노출하지 않도록 사용하는 application 결과 모델.
 */
public record ProvisionedUserResult(
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

    public static ProvisionedUserResult from(User user) {
        return new ProvisionedUserResult(
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
