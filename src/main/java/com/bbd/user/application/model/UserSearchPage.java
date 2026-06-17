package com.bbd.user.application.model;

import com.bbd.user.domain.User;

import java.util.List;

/*
 Persistence adapter가 조회한 사용자 page.

 application service가 domain User를 외부 응답용 result로 변환하기 전 단계다.
 */
public record UserSearchPage(
        List<User> users,
        long totalElements,
        int page,
        int size
) {
}