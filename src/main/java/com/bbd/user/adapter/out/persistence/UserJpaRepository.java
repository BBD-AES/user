package com.bbd.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/*
 users 테이블 조회용 Spring Data JPA Repository.

 이 인터페이스는 persistence adapter 내부에서만 사용한다.
 application 계층은 이 Repository를 직접 알지 않는다.
 */
public interface UserJpaRepository
        extends JpaRepository<UserJpaEntity, Long>, JpaSpecificationExecutor<UserJpaEntity> {

    Optional<UserJpaEntity> findByKeycloakSub(String keycloakSub);

    Optional<UserJpaEntity> findByEmployeeNumber(String employeeNumber);
}
