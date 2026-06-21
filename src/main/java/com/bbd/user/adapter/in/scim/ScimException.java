package com.bbd.user.adapter.in.scim;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 잘못된 SCIM 요청을 RFC 7644 오류로 표현하기 위한 Inbound Adapter 전용 예외.

 사용 위치:
 - ScimFilterParser의 잘못된 filter
 - ScimPatchMapper의 잘못된 op/path/value
 - ScimUserRequest의 지원하지 않는 role/tenancy 값
 - externalId 변경처럼 SCIM 속성 규칙을 위반한 요청

 status:
 반환할 HTTP 상태 코드.

 scimType:
 invalidFilter, invalidPath, invalidValue, mutability 같은 SCIM 표준 오류 분류.

 detail:
 midPoint 또는 운영자가 원인을 확인할 수 있는 설명.

 이 예외는 application/domain 계층의 업무 오류가 아니다.
 ScimExceptionHandler가 받아 SCIM Error JSON으로 변환하고
 최종적으로 midPoint SCIM2 Connector가 HTTP 응답으로 받는다.
 */
@Getter
public class ScimException extends RuntimeException {

    private final HttpStatus status;
    private final String scimType;

    public ScimException(HttpStatus status, String scimType, String detail) {
        super(detail);
        this.status = status;
        this.scimType = scimType;
    }

    public static ScimException externalIdImmutable() {
        return new ScimException(
                HttpStatus.BAD_REQUEST,
                "mutability",
                "externalId(Keycloak sub)는 생성 후 변경할 수 없습니다."
        );
    }
}
