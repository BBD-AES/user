package com.bbd.user.application.service;

import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.GetUserSnapshotUseCase;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.domain.User;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 UserSnapshot 조회 유스케이스 구현체.

 공통 인가 프레임워크가 Redis miss 시 User Service의 Snapshot API를 호출하면,
 최종적으로 이 서비스가 keycloakSub 기준으로 사용자 정보를 조회한다.

 이 서비스는 JPA Repository를 직접 알지 않는다.
 application 계층은 LoadUserPort를 통해 User 도메인 모델을 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserSnapshotService implements GetUserSnapshotUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public UserSnapshotResult getSnapshotByKeycloakSub(String keycloakSub) {
        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new ApiException(ErrorCode.USER_INVALID_KEYCLOAK_SUB);
        }

        User user = loadUserPort.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return UserSnapshotResult.from(user);
    }
}