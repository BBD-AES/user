package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;

/*
 User 목록 조회 조건.

 employeeNumber와 name은 운영자가 일부 값으로 검색할 수 있게 부분 일치로 처리한다.
 role, tenancyType은 enum 값이므로 정확히 일치하는 사용자만 조회한다.
 */
public record UserSearchCondition(
        String employeeNumber,
        String name,
        UserRole role,
        TenancyType tenancyType,
        int page,
        int size
) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    public UserSearchCondition {
        page = Math.max(page, 0);

        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        size = Math.min(size, MAX_SIZE);

        employeeNumber = blankToNull(employeeNumber);
        name = blankToNull(name);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}