package com.bbd.user.adapter.in.web.response;

import com.bbd.user.application.model.UserSearchResult;

import java.util.List;

/*
 RDS 사용자 목록 조회 응답.

 users는 기존 UserSnapshotResponse를 재사용해 단건 Snapshot 조회와 목록 조회의
 사용자 표현이 서로 달라지지 않게 한다.
 */
public record UserSearchResponse(
        List<UserSnapshotResponse> users,
        long totalElements,
        int page,
        int size
) {

    public static UserSearchResponse from(UserSearchResult result) {
        return new UserSearchResponse(
                result.users().stream()
                        .map(UserSnapshotResponse::from)
                        .toList(),
                result.totalElements(),
                result.page(),
                result.size()
        );
    }
}

//{
//  "users": [
//    {
//      "userId": "Long",
//      "keycloakSub": "String",
//      "employeeNumber": "String",
//      "displayName": "String",
//      "email": "String",
//      "position": "String",
//      "status": "ACTIVE | INACTIVE | PENDING | TERMINATED",
//      "role": "ADMIN | HQ_MANAGER | HQ_STAFF | BRANCH_MANAGER | BRANCH_STAFF",
//      "tenancyType": "HQ | BRANCH",
//      "tenancyName": "String",
//      "version": "Long"
//    }
//  ],
//  "totalElements": "long",
//  "page": "int",
//  "size": "int"
//}