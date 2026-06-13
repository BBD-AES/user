package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.request.UpdateUserAuthorizationRequest;
import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.UpdateUserAuthorizationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 ERP 관리자가 사용자의 인가 정보를 변경하는 Web Adapter.

 이 API는 Gateway가 Relay한 Keycloak Access Token을 UserSecurityConfig에서 검증한 뒤 호출된다.
 Controller는 JWT sub와 HTTP 요청 값을 application command로 변환하는 역할만 한다.

 실제 권한 검사, User DB 변경, Outbox 저장은
 UpdateUserAuthorizationUseCase 구현체에서 처리한다.

 향후 midPoint는 이 관리자 API를 호출하지 않는다.
 midPoint는 /scim/** 전용 mTLS API를 사용하고,
 두 adapter가 내부 application 변경 로직과 Outbox 흐름을 재사용하는 구조로 확장한다.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UpdateUserAuthorizationUseCase updateUserAuthorizationUseCase;

    /*
     status, role, tenancy를 한 요청에서 함께 변경한다.

     @AuthenticationPrincipal Jwt:
     Resource Server 검증을 통과한 Access Token의 payload.
     sub는 변경 요청자를 User DB에서 찾는 안정적인 식별값으로 사용한다.
     */
    @PatchMapping("/{userId}/authorization")
    public UserSnapshotResponse updateAuthorization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserAuthorizationRequest request
    ) {
        UserSnapshotResult result = updateUserAuthorizationUseCase.updateAuthorization(
                new UpdateUserAuthorizationCommand(
                        jwt.getSubject(),
                        userId,
                        request.status(),
                        request.role(),
                        request.tenancyType(),
                        request.tenancyName()
                )
        );

        return UserSnapshotResponse.from(result);
    }
}
