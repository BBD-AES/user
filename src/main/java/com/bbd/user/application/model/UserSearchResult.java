package com.bbd.user.application.model;

import java.util.List;

/*
 사용자 목록 조회 유스케이스 결과.

 users 항목은 Snapshot과 같은 형태를 재사용해 security-core와 관리자 화면이
 동일한 사용자 표현을 볼 수 있게 한다.
 */
public record UserSearchResult(
        List<UserSnapshotResult> users,
        long totalElements,
        int page,
        int size
) {

    public static UserSearchResult from(UserSearchPage page) {
        return new UserSearchResult(
                page.users().stream()
                        .map(UserSnapshotResult::from)
                        .toList(),
                page.totalElements(),
                page.page(),
                page.size()
        );
    }
}