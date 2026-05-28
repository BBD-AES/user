package com.bbd.user.global.error;

import com.bbd.user.global.error.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 전역 예외처리
 * 사용법 :
 * throw new ApiException(HttpStatus.NOT_FOUND, "404", "NOT_FOUND", "유저를 찾을 수 없습니다");
 *
 *  HttpServletRequest?
 *  현재 들어온 HTTP 요청 자체를 담고있는 객체임
 *  요청 URL, HTTP method, 헤더, 쿼리 파라미터, 클라이언트 정보 조회 가능
 */

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest req) {

        ErrorResponse errorResponse = new ErrorResponse(e.getStatus(), e.getCode(), e.getMessage());

        return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);

    }

}