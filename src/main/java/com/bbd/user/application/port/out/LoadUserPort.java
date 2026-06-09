package com.bbd.user.application.port.out;

import com.bbd.user.domain.User;

import java.util.Optional;

/*
 application 계층이 사용자 저장소를 조회하기 위해 사용하는 outbound port.

 application 계층은 JPA Repository를 직접 알지 않고,
 이 포트를 통해 User 도메인 모델을 조회한다.
 */
public interface LoadUserPort {

    Optional<User> findByKeycloakSub(String keycloakSub);
}