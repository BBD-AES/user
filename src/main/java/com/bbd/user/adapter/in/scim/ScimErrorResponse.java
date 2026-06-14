package com.bbd.user.adapter.in.scim;

import java.util.List;

/*
 RFC 7644가 정의한 SCIM Error HTTP 응답 body.

 일반 ERP API는 GlobalExceptionHandler가 ProblemDetail을 반환하지만,
 /scim/** 요청은 midPoint Connector가 이해할 수 있도록 이 형식을 사용한다.

 schemas:
 이 payload가 SCIM Error임을 나타내는 고정 URN.

 status:
 RFC 형식에 맞춘 문자열 HTTP 상태 코드.

 scimType:
 오류를 더 구체적으로 분류하는 선택 필드.

 detail:
 요청 실패 원인 설명.

 내부 ErrorCode 값(U001 등)은 SCIM 계약이 아니므로 직접 노출하지 않는다.
 */
public record ScimErrorResponse(
        List<String> schemas,
        String status,
        String scimType,
        String detail
) {

    // HTTP 상태와 SCIM 오류 정보를 고정 Error schema가 포함된 응답으로 만든다.
    public static ScimErrorResponse of(int status, String scimType, String detail) {
        return new ScimErrorResponse(
                List.of(ScimConstants.ERROR_SCHEMA),
                Integer.toString(status),
                scimType,
                detail
        );
    }
}
