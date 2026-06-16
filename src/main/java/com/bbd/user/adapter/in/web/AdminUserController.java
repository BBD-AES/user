package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.request.UpdateUserAuthorizationRequest;
import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.SearchUsersUseCase;
import com.bbd.user.application.port.in.UpdateUserAuthorizationUseCase;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/*
 ERP 관리자가 사용자의 인가 정보를 변경하는 Web Adapter.

 이 API는 Gateway가 Relay한 Keycloak Access Token을 UserSecurityConfig에서 검증한 뒤 호출된다.
 Controller는 JWT sub와 HTTP 요청 값을 application command로 변환하는 역할만 한다.

 실제 권한 검사, User DB 변경, Outbox 저장은
 UpdateUserAuthorizationUseCase 구현체에서 처리한다.

 midPoint는 이 관리자 API를 호출하지 않는다.
 midPoint는 /scim/** 전용 mTLS API를 사용하고,
 두 adapter는 각자의 application use case에서 같은 Outbox와 Snapshot 복구 흐름을 사용한다.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SearchUsersUseCase searchUsersUseCase;
    private final UpdateUserAuthorizationUseCase updateUserAuthorizationUseCase;


    /*
  RDS users 테이블 기준으로 사용자 목록을 조회한다.

  지원 조건:
  - 조건 없음: 전체 목록
  - employeeNumber: 사번 부분 일치
  - name: displayName 또는 username 부분 일치
  - role: 역할 정확히 일치
  - tenancyType: HQ/BRANCH 정확히 일치
  - role + tenancyType: 지점/본사 안에서 역할별 조회
  */
    @GetMapping
    public UserSearchResponse searchUsers(
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) TenancyType tenancyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return UserSearchResponse.from(
                searchUsersUseCase.search(
                        new UserSearchCondition(
                                employeeNumber,
                                name,
                                role,
                                tenancyType,
                                page,
                                size
                        )
                )
        );
    }

    /*
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
