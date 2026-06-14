package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;

/*
 SCIM PUT/PATCH로 전달된 사용자 원천 정보 변경 명령.

 null 필드는 변경하지 않는다.
 active=true는 ERP 승인 명령이 아니므로 기존 ACTIVE/PENDING 상태를 유지하고,
 INACTIVE 사용자만 재승인 대상인 PENDING으로 되돌린다.
 */
public record UpdateProvisionedUserCommand(
        Long userId,
        String employeeNumber,
        String username,
        String displayName,
        String email,
        String position,
        UserRole role,
        TenancyType tenancyType,
        String tenancyName,
        Boolean sourceActive
) {
}
