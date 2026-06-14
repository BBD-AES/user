package com.bbd.user.application.model;

/*
 SCIM 목록 조회와 reconciliation 검색을 application 계층으로 전달한다.

 field와 value가 null이면 전체 목록을 pagination해서 조회한다.
 startIndex는 SCIM 규격에 맞춰 1부터 시작한다.
 */
public record SearchProvisionedUsersCommand(
        ProvisionedUserSearchField field,
        String value,
        int startIndex,
        int count
) {
}
