package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;

/*
 User 목록 조회 조건.

 employeeNumber와 name은 운영자가 일부 값으로 검색할 수 있게 부분 일치로 처리한다.
 role, tenancyType은 enum 값이므로 정확히 일치하는 사용자만 조회한다.

 생성 시점에 page, size, 검색어를 보정해서
 application/service 계층에서 별도 방어 로직 없이 안전하게 사용할 수 있게 한다.
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
        /*
         page는 음수가 들어와도 0페이지로 보정한다.
         */
        page = Math.max(page, 0);

        /*
         size가 0 이하이면 기본값을 사용하고,
         과도하게 큰 size 요청은 최대 200건으로 제한한다.
         */
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        size = Math.min(size, MAX_SIZE);

        /*
         공백 검색어는 조건 없음(null)으로 처리한다.
         앞뒤 공백은 제거해서 검색 조건을 정규화한다.
         */
        employeeNumber = blankToNull(employeeNumber);
        name = blankToNull(name);
    }

    /*
     null, 빈 문자열, 공백 문자열은 검색 조건에서 제외하기 위해 null로 변환한다.
     실제 값이 있으면 앞뒤 공백을 제거한 문자열을 반환한다.
     */
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}