package com.bbd.user.adapter.out.persistence;

import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/*
 User 조회 persistence adapter.

 application 계층의 LoadUserPort를 구현한다.
 JPA Repository로 DB를 조회한 뒤 User 도메인 모델로 변환해서 반환한다.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return userJpaRepository.findByKeycloakSub(keycloakSub)
                .map(UserJpaEntity::toDomain);
    }
}