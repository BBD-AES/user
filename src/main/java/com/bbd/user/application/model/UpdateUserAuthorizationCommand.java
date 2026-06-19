package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

/*
 사용자 권한 변경 유스케이스의 입력 모델.

 Web Controller의 Request DTO를 application 계층까지 직접 전달하지 않기 위해 사용한다.

 targetUserId:
 실제로 상태, 역할, 소속을 변경할 ERP 사용자 ID.

 SCIM adapter도 protocol 입력을 별도 application command로 변환하고,
 관리자 API와 같은 Outbox 및 Snapshot 복구 흐름을 사용한다.
 */
public record UpdateUserAuthorizationCommand(
        Long targetUserId,
        UserStatus status,
        UserRole role,
        TenancyType tenancyType,
        String tenancyName
) {
}
