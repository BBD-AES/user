package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.in.GetUserSnapshotUseCase;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 내부 서비스용 UserSnapshot 조회 API.

 공통 인가 프레임워크가 Redis UserSnapshot cache miss 시 이 API를 호출한다.
 User Service가 users DB를 조회한 뒤,
 인가 판단에 필요한 사용자 정보를 UserSnapshot 형태로 변환해 제공하는 API이다.

 MSA가 현재 사용자의 Access Token을 Relay해서 호출하는 구조이므로,
 JWT sub와 조회하려는 keycloakSub가 같을 때만 응답한다.
 다른 사용자의 keycloakSub를 임의로 지정하는 조회는 AUTH_FORBIDDEN으로 차단한다.

 향후 사용자 요청과 관계없는 시스템 호출이 필요하면
 client_credentials 기반 서비스 계정과 별도 scope 검증을 추가해야 한다.
 */
@Tag(name = "6. Internal Snapshot Controller")
@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalUserSnapshotController {

    private final GetUserSnapshotUseCase getUserSnapshotUseCase;

    @Operation(summary = "내부 서비스용 사용자 스냅샷 조회 API")
    @GetMapping("/api/v1/users/internal/snapshot")
    public UserSnapshotResponse getSnapshot(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String keycloakSub
    ) {

        // Relay된 Access Token의 사용자와 Snapshot 조회 대상이 같은지 확인한다.
        if (jwt == null || !jwt.getSubject().equals(keycloakSub)) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN);
        }

        UserResult result = getUserSnapshotUseCase.getSnapshotByKeycloakSub(keycloakSub);
        log.info("{}를 유저 스냅샷을 통해 조회하였습니다.", result.displayName());
        return UserSnapshotResponse.from(result);
    }
}
