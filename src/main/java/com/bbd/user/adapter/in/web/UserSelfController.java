package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.in.GetUserSnapshotUseCase;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. User Directory Controller")
@RestController
@RequiredArgsConstructor
public class UserSelfController {

    private final GetUserSnapshotUseCase getUserSnapshotUseCase;

    @Operation(summary = "내 사용자 정보 조회 API")
    @GetMapping("/api/v1/users/me")
    public UserSnapshotResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ApiException(ErrorCode.AUTH_UNAUTHENTICATED);
        }

        UserResult result = getUserSnapshotUseCase.getSnapshotByKeycloakSub(jwt.getSubject());
        return UserSnapshotResponse.from(result);
    }
}
