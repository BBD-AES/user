package com.bbd.user.adapter.in.scim;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/*
 midPoint가 연결 초기화와 schema 확인에 사용하는 SCIM discovery endpoint.

 midPoint SCIM2 Connector는 User API를 호출하기 전에 다음 정보를 확인할 수 있다.

 - ServiceProviderConfig: PATCH, filter, ETag, 인증 방식 지원 여부
 - ResourceTypes: /Users endpoint와 연결된 schema
 - Schemas: User Service가 제공하는 속성 이름과 자료형

 discovery 응답은 실제 User 데이터를 변경하지 않는다.
 하지만 /scim/** 아래에 있으므로 UserSecurityConfig의 mTLS 인증을 동일하게 적용받는다.
 */
@RestController
@RequestMapping(
        value = "/scim/v2",
        produces = {ScimConstants.MEDIA_TYPE, APPLICATION_JSON_VALUE}
)
public class ScimDiscoveryController {

    /*
     User Service가 실제 구현한 SCIM 기능만 supported=true로 광고한다.

     Bulk, 비밀번호 변경, 정렬은 구현하지 않았으므로 false이고,
     인증 방식은 UserSecurityConfig와 맞춰 X.509 mutual TLS로 알린다.
     */
    @GetMapping("/ServiceProviderConfig")
    public Map<String, Object> serviceProviderConfig() {
        return Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"),
                "patch", Map.of("supported", true),
                "bulk", Map.of("supported", false, "maxOperations", 0, "maxPayloadSize", 0),
                "filter", Map.of("supported", true, "maxResults", 200),
                "changePassword", Map.of("supported", false),
                "sort", Map.of("supported", false),
                "etag", Map.of("supported", true),
                "authenticationSchemes", List.of(
                        Map.of(
                                "type", "x509",
                                "name", "Mutual TLS",
                                "description", "midPoint client certificate authentication"
                        )
                )
        );
    }

    /*
     User Service의 실제 provisioning 대상은 User다.

     사용 중인 SCIM2 Connector 1.2.9는 연결 검사에서 User와 Group ResourceType을
     모두 요구하므로 Group도 호환용 read-only resource로 광고한다.
     ERP 권한은 Group projection이 아니라 User.roles와 ERP extension으로 관리한다.
     */
    @GetMapping("/ResourceTypes")
    public ScimListResponse<Map<String, Object>> resourceTypes() {
        Map<String, Object> user = Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                "id", "User",
                "name", "User",
                "endpoint", "/Users",
                "schema", ScimConstants.CORE_USER_SCHEMA,
                "schemaExtensions", List.of(
                        Map.of("schema", ScimConstants.ENTERPRISE_USER_SCHEMA, "required", true),
                        Map.of("schema", ScimConstants.ERP_USER_SCHEMA, "required", true)
                )
        );

        Map<String, Object> group = Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                "id", "Group",
                "name", "Group",
                "endpoint", "/Groups",
                "schema", ScimConstants.CORE_GROUP_SCHEMA,
                "schemaExtensions", List.of()
        );

        return ScimListResponse.of(List.of(user, group), 2, 1);
    }

    /*
     Core, Enterprise, BBD ERP schema의 주요 속성을 반환한다.

     이 응답은 connector가 attribute definition을 확인하기 위한 metadata다.
     실제 요청 검증과 도메인 변환은 ScimUserRequest/ScimPatchMapper가 담당한다.
     */
    @GetMapping("/Schemas")
    public ScimListResponse<Map<String, Object>> schemas() {
        List<Map<String, Object>> resources = List.of(
                schema(
                        ScimConstants.CORE_USER_SCHEMA,
                        "User",
                        List.of(
                                attribute("userName", "string", true, "server"),
                                attribute("externalId", "string", true, "server"),
                                attribute("displayName", "string", false, "none"),
                                attribute("title", "string", false, "none"),
                                attribute("active", "boolean", false, "none"),
                                attribute("roles", "complex", false, "none")
                        )
                ),
                schema(
                        ScimConstants.CORE_GROUP_SCHEMA,
                        "Group",
                        List.of(
                                attribute("displayName", "string", true, "server"),
                                attribute("externalId", "string", false, "server"),
                                attribute("members", "complex", false, "none")
                        )
                ),
                schema(
                        ScimConstants.ENTERPRISE_USER_SCHEMA,
                        "EnterpriseUser",
                        List.of(
                                attribute("employeeNumber", "string", true, "server"),
                                attribute("organization", "string", true, "none"),
                                attribute("department", "string", false, "none")
                        )
                ),
                schema(
                        ScimConstants.ERP_USER_SCHEMA,
                        "BbdErpUser",
                        List.of(
                                attribute("role", "string", true, "none"),
                                attribute("tenancyType", "string", true, "none"),
                                attribute("tenancyName", "string", false, "none"),
                                attribute("approvalStatus", "string", false, "none")
                        )
                )
        );

        return ScimListResponse.of(resources, resources.size(), 1);
    }

    // Schema discovery 항목의 공통 구조를 생성한다.
    private Map<String, Object> schema(
            String id,
            String name,
            List<Map<String, Object>> attributes
    ) {
        return Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:Schema"),
                "id", id,
                "name", name,
                "description", name + " schema supported by BBD User Service",
                "attributes", attributes
        );
    }

    /*
     개별 SCIM 속성 metadata를 생성한다.

     uniqueness의 server는 중복 여부를 User Service가 관리한다는 의미이고,
     none은 고유성을 보장하지 않는 일반 속성이라는 의미다.
     */
    private Map<String, Object> attribute(
            String name,
            String type,
            boolean required,
            String uniqueness
    ) {
        return Map.of(
                "name", name,
                "type", type,
                "multiValued", false,
                "required", required,
                "caseExact", false,
                "mutability", "readWrite",
                "returned", "default",
                "uniqueness", uniqueness
        );
    }
}
