package com.bbd.user.adapter.in.scim;

/*
 User Service SCIM Adapter가 사용하는 표준 식별값 모음.

 SCIM 요청과 응답은 일반 JSON이 아니라 application/scim+json을 사용한다.
 schemas 필드에는 아래 URN을 넣어 현재 payload가 User, 목록, PATCH, 오류 중
 어떤 SCIM 문서인지 midPoint에 알려준다.

 CORE_USER_SCHEMA:
 RFC 7643의 표준 사용자 속성(userName, active, emails, roles 등).

 ENTERPRISE_USER_SCHEMA:
 RFC 7643 Enterprise 확장의 사번, 조직, 부서 속성.

 ERP_USER_SCHEMA:
 BBD ERP에서만 사용하는 역할, tenancy, 승인 상태 확장.

 이 상수는 SCIM Inbound Adapter에서만 사용하며
 application/domain 계층에는 SCIM URN이나 media type을 전달하지 않는다.
 */
public final class ScimConstants {

    public static final String MEDIA_TYPE = "application/scim+json";

    public static final String CORE_USER_SCHEMA =
            "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String ENTERPRISE_USER_SCHEMA =
            "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
    public static final String ERP_USER_SCHEMA =
            "urn:bbd:params:scim:schemas:extension:erp:2.0:User";

    public static final String LIST_RESPONSE_SCHEMA =
            "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    public static final String PATCH_OPERATION_SCHEMA =
            "urn:ietf:params:scim:api:messages:2.0:PatchOp";
    public static final String ERROR_SCHEMA =
            "urn:ietf:params:scim:api:messages:2.0:Error";

    private ScimConstants() {
    }
}
