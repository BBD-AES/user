package com.bbd.user.domain;

/*
 UserSnapshot에 포함될 대표 업무 역할.

 초기에는 role 기반으로 큰 권한을 구분하고,
 이후 세부 API 권한은 permission 목록으로 확장할 수 있다.
 */
public enum UserRole {
    // ERP 전체 설정과 사용자 권한을 관리하는 시스템 관리자.
    ADMIN,
    HQ_MANAGER,
    HQ_STAFF,
    BRANCH_MANAGER,
    BRANCH_STAFF
}
