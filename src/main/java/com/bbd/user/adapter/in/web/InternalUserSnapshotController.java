package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.GetUserSnapshotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 내부 서비스용 UserSnapshot 조회 API.

 공통 인가 프레임워크가 Redis UserSnapshot cache miss 시 이 API를 호출한다.
 User Service가 users DB를 조회한 뒤,
 인가 판단에 필요한 사용자 정보를 UserSnapshot 형태로 변환해 제공하는 API이다.
 */
@RestController
@RequiredArgsConstructor
public class InternalUserSnapshotController {

    private final GetUserSnapshotUseCase getUserSnapshotUseCase;

    @GetMapping("/api/v1/users/internal/snapshot")
    public UserSnapshotResponse getSnapshot(
            @RequestParam String keycloakSub
    ) {
        UserSnapshotResult result = getUserSnapshotUseCase.getSnapshotByKeycloakSub(keycloakSub);

        return UserSnapshotResponse.from(result);
    }
}