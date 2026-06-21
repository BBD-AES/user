package com.bbd.user.adapter.in.web;

import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.user.adapter.in.web.request.UpdateUserAuthorizationRequest;
import com.bbd.user.adapter.in.web.request.UpdateUserStatusRequest;
import com.bbd.user.adapter.in.web.response.UserSearchResponse;
import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UpdateUserStatusCommand;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.port.in.SearchUsersUseCase;
import com.bbd.user.application.port.in.UpdateUserAuthorizationUseCase;
import com.bbd.user.application.port.in.UpdateUserStatusUseCase;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequireRole(com.bbd.securitycore.domain.UserRole.ADMIN)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SearchUsersUseCase searchUsersUseCase;
    private final UpdateUserAuthorizationUseCase updateUserAuthorizationUseCase;
    private final UpdateUserStatusUseCase updateUserStatusUseCase;

    /*
    RDS users 테이블 기준으로 사용자 목록을 조회한다.

    조건 없이 호출하면 전체 사용자를 페이지 단위로 조회한다.
    사번, 이름, 역할, 소속 유형 기준으로 필터링할 수 있다.
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


    // status 변경 api
    @PatchMapping("/{userId}/status")
    public UserSnapshotResponse updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        // 사용자 변경 모델
        UserResult result = updateUserStatusUseCase.updateStatus(
                new UpdateUserStatusCommand(
                        userId,
                        request.status()
                )
        );

        // 사용자 변경 모델을
        return UserSnapshotResponse.from(result);
    }
}
