package com.bbd.user.adapter.in.scim;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/*
 RFC 7644 목록 조회와 reconciliation 검색 결과 응답.

 SCIM은 배열만 바로 반환하지 않고 ListResponse schema와 함께 다음 값을 요구한다.

 - totalResults: 조건에 맞는 전체 resource 개수
 - startIndex: 1부터 시작하는 현재 페이지의 첫 위치
 - itemsPerPage: 이번 응답에 실제 포함된 resource 개수
 - Resources: User, ResourceType, Schema 등의 실제 목록

 제네릭으로 만들어 User 검색뿐 아니라 discovery endpoint에서도 동일한 형식을 사용한다.
 */
public record ScimListResponse<T>(
        List<String> schemas,
        long totalResults,
        int startIndex,
        int itemsPerPage,
        @JsonProperty("Resources")
        List<T> resources
) {

    /*
     호출 측이 SCIM 고정 필드를 반복해서 만들지 않도록 제공하는 생성 메서드.
     itemsPerPage는 요청 count가 아니라 실제 반환된 resources 크기로 설정한다.
     */
    public static <T> ScimListResponse<T> of(
            List<T> resources,
            long totalResults,
            int startIndex
    ) {
        return new ScimListResponse<>(
                List.of(ScimConstants.LIST_RESPONSE_SCHEMA),
                totalResults,
                startIndex,
                resources.size(),
                resources
        );
    }
}
