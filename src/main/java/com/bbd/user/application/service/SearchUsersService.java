package com.bbd.user.application.service;

import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSearchResult;
import com.bbd.user.application.port.in.SearchUsersUseCase;
import com.bbd.user.application.port.out.LoadUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 RDS 기준 사용자 목록 조회 서비스.

 Redis Snapshot cache와 무관하게 users 테이블을 직접 조회하므로,
 Redis 장애 상태에서도 관리자 조회 API는 DB 기준으로 응답할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class SearchUsersService implements SearchUsersUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    @Transactional(readOnly = true)
    public UserSearchResult search(UserSearchCondition condition) {
        return UserSearchResult.from(loadUserPort.searchUsers(condition));
    }
}