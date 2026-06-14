package com.bbd.user.adapter.in.scim;

import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
 SCIM Controller에서 발생한 예외를 RFC 7644 SCIM Error 응답으로 번역한다.

 User Service의 공통 예외 정책은 그대로 유지한다.

 일반 ERP API:
 ApiException + ErrorCode
 -> GlobalExceptionHandler
 -> ProblemDetail

 SCIM API:
 ApiException + ErrorCode 또는 ScimException
 -> ScimExceptionHandler
 -> ScimErrorResponse
 -> midPoint SCIM2 Connector

 assignableTypes로 적용 대상을 ScimUserController와 ScimDiscoveryController로 제한한다.
 따라서 이 Handler는 일반 /api/** 응답 형식에 영향을 주지 않는다.

 HIGHEST_PRECEDENCE는 SCIM Controller에서 GlobalExceptionHandler보다 이 번역기를
 먼저 선택하게 하여 같은 ApiException도 SCIM 규격으로 반환하기 위한 설정이다.

 mTLS 인증 실패처럼 Controller 진입 전에 Security Filter에서 발생하는 오류는
 이 Handler가 아니라 Spring Security의 인증/인가 처리기가 담당한다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        ScimUserController.class,
        ScimDiscoveryController.class
})
public class ScimExceptionHandler {

    /*
     SCIM 문법 또는 속성 규칙 위반을 그대로 SCIM 상태와 scimType으로 반환한다.
     */
    @ExceptionHandler(ScimException.class)
    public ResponseEntity<ScimErrorResponse> handleScimException(ScimException exception) {
        return response(
                exception.getStatus(),
                exception.getScimType(),
                exception.getMessage()
        );
    }

    /*
     application 계층에서 공통 ApiException/ErrorCode로 표현한 업무 오류를 번역한다.

     사용자 없음 등의 상태와 메시지는 ErrorCode를 재사용하고,
     중복 사용자는 midPoint가 의미를 알 수 있도록 SCIM uniqueness로 변환한다.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ScimErrorResponse> handleApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        String scimType = errorCode.getStatus() == HttpStatus.CONFLICT
                ? "uniqueness"
                : null;

        return response(errorCode.getStatus(), scimType, errorCode.getMessage());
    }

    /*
     예상하지 못한 구현 오류의 세부 정보와 stack trace를 외부에 노출하지 않는다.
     midPoint에는 일반적인 500 SCIM Error만 반환하고 실제 원인은 서버 로그에서 확인한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ScimErrorResponse> handleUnexpected(Exception exception) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                null,
                "SCIM 요청 처리 중 서버 오류가 발생했습니다."
        );
    }

    /*
     모든 SCIM 오류 응답에 application/scim+json Content-Type과
     RFC 7644 Error schema를 일관되게 적용한다.
     */
    private ResponseEntity<ScimErrorResponse> response(
            HttpStatus status,
            String scimType,
            String detail
    ) {
        return ResponseEntity
                .status(status)
                .header("Content-Type", ScimConstants.MEDIA_TYPE)
                .body(ScimErrorResponse.of(status.value(), scimType, detail));
    }
}
