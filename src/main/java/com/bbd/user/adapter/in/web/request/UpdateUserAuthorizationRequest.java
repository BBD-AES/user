package com.bbd.user.adapter.in.web.request;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

/*
 사용자 인가 정보 변경 API의 HTTP request DTO.

 status, role, tenancyType은 실제 인가 판단에 사용되므로 필수값이다.
 tenancyName은 화면 표시와 로그 용도이므로 선택값으로 둔다.

 HTTP adapter 전용 객체이며 application 계층에서는
 UpdateUserAuthorizationCommand로 변환해서 사용한다.
 */
public record UpdateUserAuthorizationRequest(
        @NotNull UserStatus status,
        @NotNull UserRole role,
        @NotNull TenancyType tenancyType,
        String tenancyName
) {
}
