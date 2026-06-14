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

    private final ScimFilterParser filterParser = new ScimFilterParser();
    private final ScimPatchMapper patchMapper = new ScimPatchMapper();

    @Test
    void parsesExternalIdFilterForMidpointReconciliation() {
        ScimFilterParser.ParsedFilter parsed =
                filterParser.parse("externalId eq \"keycloak-sub\"");

        assertEquals(ProvisionedUserSearchField.KEYCLOAK_SUB, parsed.field());
        assertEquals("keycloak-sub", parsed.value());
    }

    @Test
    void rejectsUnsupportedFilter() {
        assertThrows(
                ScimException.class,
                () -> filterParser.parse("displayName co \"홍\"")
        );
    }

    @Test
    void mapsPatchOperationsToProvisioningCommand() {
        ScimPatchRequest request = new ScimPatchRequest(
                List.of(ScimConstants.PATCH_OPERATION_SCHEMA),
                List.of(
                        new ScimPatchRequest.Operation("replace", "active", false),
                        new ScimPatchRequest.Operation(
                                "replace",
                                ScimConstants.ERP_USER_SCHEMA + ":role",
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

        UpdateProvisionedUserCommand command = patchMapper.toCommand(10L, request);

        assertEquals(10L, command.userId());
        assertEquals(false, command.sourceActive());
        assertEquals(UserRole.HQ_STAFF, command.role());
        assertEquals(TenancyType.HQ, command.tenancyType());
        assertEquals("본사", command.tenancyName());
    }
}
