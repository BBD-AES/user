package com.bbd.user.adapter.in.scim;

import com.bbd.user.application.model.UpdateProvisionedUserCommand;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 SCIM PATCH operation을 application update command로 변환한다.

 SCIM adapter 안에서만 protocol path를 해석하고 application 계층에는
 HTTP/SCIM 형식을 모르는 일반 변경 명령만 전달한다.

 처리 흐름:
 1. Operations 존재 여부와 op(add/replace/remove)를 검증한다.
 2. SCIM 표준 속성 및 Enterprise/ERP 확장 경로를 해석한다.
 3. 여러 operation의 변경값을 Values에 모은다.
 4. 최종 결과를 UpdateProvisionedUserCommand로 변환한다.

 PATCH에 포함되지 않은 필드는 null로 남는다.
 application service는 null인 필드를 기존 값으로 유지하므로
 PATCH 요청이 지정한 속성만 변경된다.

 externalId는 Keycloak sub와 연결된 사용자 생명주기 식별자이므로
 생성 후 변경을 허용하지 않고 SCIM mutability 오류를 반환한다.
 */
@Component
public class ScimPatchMapper {

    /*
     SCIM PatchOp 하나에 여러 operation이 들어올 수 있으므로
     모든 operation을 먼저 적용한 뒤 하나의 application command로 만든다.
     */
    public UpdateProvisionedUserCommand toCommand(Long userId, ScimPatchRequest request) {
        if (request == null || request.operations() == null || request.operations().isEmpty()) {
            throw invalidValue("PATCH Operations가 비어 있습니다.");
        }

        Values values = new Values();

        for (ScimPatchRequest.Operation operation : request.operations()) {
            apply(values, operation);
        }

        return values.toCommand(userId);
    }

    private void apply(Values values, ScimPatchRequest.Operation operation) {
        String op = operation.op() == null
                ? ""
                : operation.op().toLowerCase(Locale.ROOT);

        if (!List.of("add", "replace", "remove").contains(op)) {
            throw invalidValue("지원하지 않는 PATCH op입니다: " + operation.op());
        }

        if (operation.path() == null || operation.path().isBlank()) {
            /*
             path 없는 add/replace는 value 객체의 각 key를 SCIM 경로로 간주한다.
             remove는 어떤 속성을 지울지 알 수 없으므로 path를 반드시 요구한다.
             */
            if ("remove".equals(op)) {
                throw invalidValue("path 없는 remove operation은 지원하지 않습니다.");
            }
            applyMap(values, operation.valueAsMap());
            return;
        }

        String path = operation.path();
        Object value = "remove".equals(op) ? "" : operation.value();
        applyPath(values, path, value);
    }

    private void applyMap(Values values, Map<String, Object> map) {
        /*
         Enterprise/ERP extension은 URN을 key로 갖는 중첩 객체다.
         중첩 Map을 "schemaURN:attribute" 경로로 펼쳐서 applyPath에서 동일하게 처리한다.
         */
        map.forEach((path, value) -> {
            if (ScimConstants.ENTERPRISE_USER_SCHEMA.equals(path) && value instanceof Map<?, ?> nested) {
                applyNested(values, ScimConstants.ENTERPRISE_USER_SCHEMA, nested);
            } else if (ScimConstants.ERP_USER_SCHEMA.equals(path) && value instanceof Map<?, ?> nested) {
                applyNested(values, ScimConstants.ERP_USER_SCHEMA, nested);
            } else if ("name".equalsIgnoreCase(path) && value instanceof Map<?, ?> nested) {
                Object formatted = nested.get("formatted");
                if (formatted != null) {
                    values.displayName = formatted.toString();
                }
            } else {
                applyPath(values, path, value);
            }
        });
    }

    private void applyNested(Values values, String prefix, Map<?, ?> map) {
        map.forEach((key, value) -> applyPath(values, prefix + ":" + key, value));
    }

    private void applyPath(Values values, String path, Object value) {
        /*
         SCIM 외부 속성을 User Service 내부 변경 필드로 번역한다.

         표준 roles와 Enterprise organization/department를 우선 지원해
         midPoint의 일반 SCIM2 Connector가 ERP 전용 schema 없이도 동작할 수 있게 한다.
         ERP extension 경로도 같은 내부 role/tenancy 필드로 연결한다.
         */
        if (ScimConstants.ENTERPRISE_USER_SCHEMA.equalsIgnoreCase(path)
                && value instanceof Map<?, ?> map) {
            applyNested(values, ScimConstants.ENTERPRISE_USER_SCHEMA, map);
            return;
        }
        if (ScimConstants.ERP_USER_SCHEMA.equalsIgnoreCase(path)
                && value instanceof Map<?, ?> map) {
            applyNested(values, ScimConstants.ERP_USER_SCHEMA, map);
            return;
        }
        if ("externalId".equalsIgnoreCase(path)) {
            throw ScimException.externalIdImmutable();
        }
        if ("userName".equalsIgnoreCase(path)) {
            values.employeeNumber = stringValue(value);
            return;
        }
        if ("displayName".equalsIgnoreCase(path) || "name.formatted".equalsIgnoreCase(path)) {
            values.displayName = stringValue(value);
            return;
        }
        if ("title".equalsIgnoreCase(path)) {
            values.position = stringValue(value);
            return;
        }
        if ("active".equalsIgnoreCase(path)) {
            values.sourceActive = booleanValue(value);
            return;
        }
        if (path.toLowerCase(Locale.ROOT).startsWith("emails")) {
            values.email = emailValue(value);
            return;
        }
        if (path.toLowerCase(Locale.ROOT).startsWith("roles")) {
            values.role = roleValue(value);
            return;
        }
        if ((ScimConstants.ENTERPRISE_USER_SCHEMA + ":employeeNumber")
                .equalsIgnoreCase(path)) {
            values.employeeNumber = stringValue(value);
            return;
        }
        if ((ScimConstants.ENTERPRISE_USER_SCHEMA + ":organization")
                .equalsIgnoreCase(path)) {
            values.tenancyType = enumValue(TenancyType.class, value);
            return;
        }
        if ((ScimConstants.ENTERPRISE_USER_SCHEMA + ":department")
                .equalsIgnoreCase(path)) {
            values.tenancyName = stringValue(value);
            return;
        }
        if ((ScimConstants.ERP_USER_SCHEMA + ":role").equalsIgnoreCase(path)) {
            values.role = enumValue(UserRole.class, value);
            return;
        }
        if ((ScimConstants.ERP_USER_SCHEMA + ":tenancyType").equalsIgnoreCase(path)) {
            values.tenancyType = enumValue(TenancyType.class, value);
            return;
        }
        if ((ScimConstants.ERP_USER_SCHEMA + ":tenancyName").equalsIgnoreCase(path)) {
            values.tenancyName = stringValue(value);
            return;
        }

        throw new ScimException(
                HttpStatus.BAD_REQUEST,
                "invalidPath",
                "지원하지 않는 PATCH path입니다: " + path
        );
    }

    private String emailValue(Object value) {
        /*
         SCIM emails는 multi-valued 속성이므로 문자열, 단일 객체, 객체 배열 형태를
         모두 받을 수 있다. 현재 User DB는 이메일 하나만 저장하므로 첫 값을 사용한다.
         */
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Map<?, ?> map) {
            Object email = map.get("value");
            return email == null ? "" : email.toString();
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            return emailValue(list.getFirst());
        }
        return "";
    }

    private UserRole roleValue(Object value) {
        /*
         roles 역시 SCIM에서는 배열이지만 ERP는 대표 역할 하나를 사용한다.
         첫 role의 value를 UserRole enum으로 변환한다.
         */
        if (value instanceof Map<?, ?> map) {
            return enumValue(UserRole.class, map.get("value"));
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            return roleValue(list.getFirst());
        }
        return enumValue(UserRole.class, value);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        throw invalidValue("active 값은 boolean이어야 합니다.");
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        /*
         SCIM에서 들어온 문자열을 도메인 enum으로 바꾸는 Adapter 경계 변환이다.
         알 수 없는 값은 내부 IllegalArgumentException을 노출하지 않고
         midPoint가 이해할 수 있는 SCIM invalidValue 오류로 바꾼다.
         */
        try {
            return Enum.valueOf(type, stringValue(value).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidValue(type.getSimpleName() + " 값이 올바르지 않습니다: " + value);
        }
    }

    private ScimException invalidValue(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", detail);
    }

    private static class Values {
        /*
         여러 PATCH operation에서 수집한 변경 후보.

         null은 "요청에서 해당 필드를 변경하지 않음"을 의미한다.
         remove는 현재 문자열 필드에 빈 문자열을 전달하며,
         role/tenancy처럼 필수인 enum 필드는 유효한 값만 허용한다.
         */
        private String employeeNumber;
        private String displayName;
        private String email;
        private String position;
        private UserRole role;
        private TenancyType tenancyType;
        private String tenancyName;
        private Boolean sourceActive;

        private UpdateProvisionedUserCommand toCommand(Long userId) {
            return new UpdateProvisionedUserCommand(
                    userId,
                    employeeNumber,
                    displayName,
                    email,
                    position,
                    role,
                    tenancyType,
                    tenancyName,
                    sourceActive
            );
        }
    }
}
