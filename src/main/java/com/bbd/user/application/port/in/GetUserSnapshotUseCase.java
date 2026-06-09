package com.bbd.user.application.port.in;

import com.bbd.user.application.model.UserSnapshotResult;

/*
 UserSnapshot 조회 유스케이스.

 공통 인가 프레임워크가 Redis miss 시 User Service에 요청할
 Snapshot 데이터를 조회하는 application 진입점이다.
 */
public interface GetUserSnapshotUseCase {

    UserSnapshotResult getSnapshotByKeycloakSub(String keycloakSub);
}