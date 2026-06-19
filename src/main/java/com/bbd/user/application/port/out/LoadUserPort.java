package com.bbd.user.application.port.out;

import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSearchPage;
import com.bbd.user.domain.User;

import java.util.List;
import java.util.Optional;

/*
 application 계층이 사용자 저장소를 조회하기 위해 사용하는 outbound port.

 application 계층은 JPA Repository를 직접 알지 않고,
 이 포트를 통해 User 도메인 모델을 조회한다.
 */
public interface LoadUserPort {

    // JWT sub를 기준으로 현재 요청자나 인증 대상 사용자를 조회한다.
    Optional<User> findByKeycloakSub(String keycloakSub);

    // 관리자 변경 API의 path variable로 받은 ERP 사용자 ID를 조회한다.
    Optional<User> findById(Long userId);

    default Optional<User> findByEmployeeNumber(String employeeNumber) {
        return Optional.empty();
    }

    default List<User> findAll(int offset, int count) {
        return List.of();
    }

    default long countAll() {
        return 0L;
    }

    default UserSearchPage searchUsers(UserSearchCondition condition) {
        return new UserSearchPage(List.of(), 0L, condition.page(), condition.size());
    }
}
