package com.bbd.user.domain;

/*
 UserSnapshot에 포함될 대표 업무 역할.

 초기에는 role 기반으로 큰 권한을 구분하고,
 이후 세부 API 권한은 permission 목록으로 확장할 수 있다.
 */
public enum UserRole {
    HQ_MANAGER,
    HQ_STAFF,
    BRANCH_MANAGER,
    BRANCH_STAFF
}