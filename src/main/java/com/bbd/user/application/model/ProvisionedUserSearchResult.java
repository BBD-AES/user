package com.bbd.user.application.model;

import java.util.List;

/*
 SCIM ListResponse의 totalResults, startIndex, itemsPerPage를 만들기 위한 조회 결과.
 */
public record ProvisionedUserSearchResult(
        List<UserResult> users,
        long totalResults,
        int startIndex
) {
}
