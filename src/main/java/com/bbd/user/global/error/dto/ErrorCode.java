package com.bbd.user.global.error.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    /*
     공통 에러
     */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COMMON001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 자원을 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다."),

    /*
     인증/인가 에러
     */
    AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증이 필요합니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH002", "토큰이 만료되었습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH003", "권한이 없습니다."),

    /*
     User Service 에러
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "U002", "비활성화된 사용자입니다."),
    USER_PENDING(HttpStatus.FORBIDDEN, "U003", "승인 대기 중인 사용자입니다."),
    USER_DUPLICATED_EMPLOYEE_NUMBER(HttpStatus.CONFLICT, "U004", "이미 등록된 사번입니다."),
    USER_DUPLICATED_KEYCLOAK_SUB(HttpStatus.CONFLICT, "U005", "이미 등록된 Keycloak 사용자입니다."),
    USER_INVALID_KEYCLOAK_SUB(HttpStatus.BAD_REQUEST, "U006", "keycloakSub는 필수입니다."),
    USER_INVALID_EMPLOYEE_NUMBER(HttpStatus.BAD_REQUEST, "U008", "employeeNumber는 필수입니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
