package com.bbd.user.application.model;

/*
 SCIM filter에서 User Service가 지원하는 exact-match 검색 필드.
 */
public enum ProvisionedUserSearchField {
    KEYCLOAK_SUB,
    EMPLOYEE_NUMBER,
    USERNAME
}
