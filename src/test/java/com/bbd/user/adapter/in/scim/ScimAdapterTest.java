package com.bbd.user.adapter.in.scim;

import com.bbd.user.application.model.ProvisionedUserSearchField;
import com.bbd.user.application.model.UpdateProvisionedUserCommand;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 SCIM protocol 값을 application 명령으로 변환하는 adapter 규칙을 검증한다.
 */
class ScimAdapterTest {

    private final ScimFilterParser filterParser =
            new ScimFilterParser();

    private final ScimPatchMapper patchMapper =
            new ScimPatchMapper();

    /*
     midPoint reconciliation 과정에서 사용하는 externalId 필터를
     keycloakSub 검색 조건으로 변환하는지 검증한다.
     */
    @Test
    void parsesExternalIdFilterForMidpointReconciliation() {
        ScimFilterParser.ParsedFilter parsed =
                filterParser.parse(
                        "externalId eq \"keycloak-sub\""
                );

        assertEquals(
                ProvisionedUserSearchField.KEYCLOAK_SUB,
                parsed.field()
        );
        assertEquals("keycloak-sub", parsed.value());
    }

    /*
     User Service가 지원하지 않는 필터 연산은 조용히 무시하지 않고
     SCIM 오류로 거절해야 한다.
     */
    @Test
    void rejectsUnsupportedFilter() {
        assertThrows(
                ScimException.class,
                () -> filterParser.parse(
                        "displayName co \"홍\""
                )
        );
    }

    /*
     SCIM Connector가 externalId를 생성 요청에 직렬화하지 않는 경우,
     호환 속성인 userType을 keycloakSub로 사용하는지 검증한다.
     */
    @Test
    void usesUserTypeAsKeycloakSubWhenConnectorOmitsExternalId() {
        ScimUserRequest request = new ScimUserRequest(
                List.of(ScimConstants.CORE_USER_SCHEMA),
                null,
                "erp-user",
                "keycloak-sub",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new ScimUserRequest.ScimRole(
                                "HQ_STAFF",
                                null,
                                true
                        )
                ),
                true,
                new ScimUserRequest.EnterpriseExtension(
                        "E-001",
                        "HQ",
                        null
                ),
                null
        );

        assertEquals(
                "keycloak-sub",
                request.toCreateCommand().keycloakSub()
        );
    }

    /*
     Connector가 roles와 Enterprise organization 같은 복합 속성을 누락하면
     nickName과 locale에 담긴 호환값을 사용하는지 검증한다.
     */
    @Test
    void usesSimpleConnectorFieldsWhenComplexAttributesAreOmitted() {
        ScimUserRequest request = new ScimUserRequest(
                List.of(ScimConstants.CORE_USER_SCHEMA),
                null,
                "erp-user",
                "keycloak-sub",
                null,
                null,
                "HQ_STAFF",
                "HQ",
                null,
                null,
                null,
                true,
                new ScimUserRequest.EnterpriseExtension(
                        "E-001",
                        null,
                        null
                ),
                null
        );

        assertEquals(
                UserRole.HQ_STAFF,
                request.toCreateCommand().role()
        );
        assertEquals(
                TenancyType.HQ,
                request.toCreateCommand().tenancyType()
        );
    }

    /*
     SCIM PATCH 요청의 active, role, tenancy 속성을
     UpdateProvisionedUserCommand로 변환하는지 검증한다.
     */
    @Test
    void mapsPatchOperationsToProvisioningCommand() {
        ScimPatchRequest request = new ScimPatchRequest(
                List.of(ScimConstants.PATCH_OPERATION_SCHEMA),
                List.of(
                        new ScimPatchRequest.Operation(
                                "replace",
                                "active",
                                false
                        ),
                        new ScimPatchRequest.Operation(
                                "replace",
                                ScimConstants.ERP_USER_SCHEMA
                                        + ":role",
                                "HQ_STAFF"
                        ),
                        new ScimPatchRequest.Operation(
                                "replace",
                                ScimConstants.ERP_USER_SCHEMA,
                                Map.of(
                                        "tenancyType", "HQ",
                                        "tenancyName", "본사"
                                )
                        )
                )
        );

        UpdateProvisionedUserCommand command =
                patchMapper.toCommand(10L, request);

        assertEquals(10L, command.userId());
        assertEquals(false, command.sourceActive());
        assertEquals(UserRole.HQ_STAFF, command.role());
        assertEquals(TenancyType.HQ, command.tenancyType());
        assertEquals("본사", command.tenancyName());
    }
}