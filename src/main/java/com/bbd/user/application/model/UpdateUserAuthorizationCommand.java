package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;

/*
 사용자 권한 변경 유스케이스의 입력 모델.

 Web Controller의 Request DTO를 application 계층까지 직접 전달하지 않기 위해 사용한다.

 actorKeycloakSub:
 JWT sub에서 가져온 변경 요청자 식별값.

 targetUserId:
 실제로 상태, 역할, 소속을 변경할 ERP 사용자 ID.

 이후 SCIM adapter가 추가되더라도 adapter별 입력을 이와 같은 application command로
 변환해서 동일한 변경 규칙과 Outbox 흐름을 재사용할 수 있다.
 */
public record UpdateUserAuthorizationCommand(
        String actorKeycloakSub,
        Long targetUserId,
        UserStatus status,
        UserRole role,
        TenancyType tenancyType,
        String tenancyName
) {
}
