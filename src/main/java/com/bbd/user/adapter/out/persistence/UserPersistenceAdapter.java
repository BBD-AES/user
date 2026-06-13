package com.bbd.user.adapter.out.persistence;

import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/*
 User 조회/수정 persistence adapter.

 application 계층의 LoadUserPort와 SaveUserPort를 구현한다.
 JPA Repository로 DB를 조회한 뒤 User 도메인 모델로 변환해서 반환한다.

 application 계층은 이 adapter를 통해서만 users 테이블에 접근하며,
 JPA Entity나 Repository를 직접 알지 않는다.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return userJpaRepository.findByKeycloakSub(keycloakSub)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        // 현재는 관리자 권한 변경만 있으므로 신규 사용자 insert는 허용하지 않는다.
        if (user.getId() == null) {
            throw new IllegalArgumentException("기존 사용자만 수정할 수 있습니다.");
        }

        UserJpaEntity entity = userJpaRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("수정할 사용자가 존재하지 않습니다."));

        entity.updateFrom(user);

        /*
         saveAndFlush를 사용해서 현재 트랜잭션 안에서 DB UPDATE와 Version 증가를 실행한다.
         반환된 User의 version을 같은 트랜잭션에 저장되는 UserChangedEvent에 사용한다.
         */
        return userJpaRepository.saveAndFlush(entity).toDomain();
    }
}
