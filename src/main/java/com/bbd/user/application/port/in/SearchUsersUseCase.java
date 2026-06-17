package com.bbd.user.application.port.in;

import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSearchResult;

/*
 RDS에 저장된 ERP 사용자 목록을 조회하는 유스케이스.
 */
public interface SearchUsersUseCase {

    UserSearchResult search(UserSearchCondition condition);
}