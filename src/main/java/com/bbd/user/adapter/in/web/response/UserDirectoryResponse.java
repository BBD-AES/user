package com.bbd.user.adapter.in.web.response;

import com.bbd.user.application.model.UserResult;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserStatus;

/*
 일반 ERP 화면에서 사용하는 사용자 기본 정보 응답.
 권한 관리 전용 값인 keycloakSub, role, version은 노출하지 않는다.
 */
public record UserDirectoryResponse(
        Long userId,
        String employeeNumber,
        String displayName,
        String email,
        String position,
        UserStatus status,
        TenancyType tenancyType,
        String tenancyName
) {

    public static UserDirectoryResponse from(UserResult result) {
        return new UserDirectoryResponse(
                result.userId(),
                result.employeeNumber(),
                result.displayName(),
                result.email(),
                result.position(),
                result.status(),
                result.tenancyType(),
                result.tenancyName()
        );
    }
}
