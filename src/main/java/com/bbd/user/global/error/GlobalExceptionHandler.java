package com.bbd.user.global.error;

import com.bbd.user.global.error.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    // 서비스에서 의도적으로 던진 ApiException을 처리한다.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(
            ApiException e,
            HttpServletRequest request
    ) {
        ProblemDetail body = e.getBody();

        body.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity
                .status(e.getStatusCode())
                .body(body);
    }

    /*
     도메인 모델이나 유스케이스에서 발생한 잘못된 인자 예외를 처리한다.

     예:
     - keycloakSub가 비어 있음
     - 필수 값 누락
     - 잘못된 enum 값
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        ProblemDetail body = ProblemDetail.forStatus(errorCode.getStatus());

        body.setTitle(errorCode.getCode());
        body.setDetail(e.getMessage());
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }

    /*
     처리되지 않은 모든 예외를 INTERNAL_ERROR로 변환한다.

     내부 예외 메시지, SQL, 스택트레이스 등은 클라이언트에 노출하지 않는다.
     실제 원인은 서버 로그에서 확인한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;

        ProblemDetail body = ProblemDetail.forStatus(errorCode.getStatus());

        body.setTitle(errorCode.getCode());
        body.setDetail(errorCode.getMessage());
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }
}