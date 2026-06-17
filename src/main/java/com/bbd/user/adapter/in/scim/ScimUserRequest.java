package com.bbd.user.adapter.in.scim;

import com.bbd.user.application.model.CreateProvisionedUserCommand;
import com.bbd.user.application.model.UpdateProvisionedUserCommand;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

/*
 SCIM User POST/PUT 요청.

 midPoint가 ERP 대상 사용자 projection을 생성하거나 갱신할 때 보내는 payload를 받는다.
 알 수 없는 속성은 무시해 SCIM Connector가 추가 표준 속성을 보내도 요청 전체가 깨지지 않게 한다.

 주요 매핑:
 - externalId: Keycloak sub. User Service와 JWT 사용자를 연결하는 불변 식별자
 - userName: 로그인 ID
 - displayName/name.formatted: 화면 표시 이름
 - title: 직급 또는 직책
 - emails: 업무 이메일
 - active: 원천 계정의 활성 여부. ERP 관리자 승인 여부와는 별개
 - Enterprise extension: 사번, HQ/BRANCH 구분, 부서 또는 지점명
 - roles 또는 ERP extension: ERP 역할 후보

 표준 SCIM2 Connector 호환성을 위해 roles와 Enterprise extension을 지원하고,
 BBD 전용 ERP extension이 함께 오면 ERP extension 값을 우선 사용한다.

 이 DTO는 SCIM Adapter의 외부 입력 모델이다.
 application service에는 DTO를 직접 전달하지 않고 Create/Update Command로 변환한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScimUserRequest(
        List<String> schemas,
        String externalId,
        String userName,
        String userType,
        String displayName,
        ScimName name,
        String nickName,
        String locale,
        String title,
        List<ScimEmail> emails,
        List<ScimRole> roles,
        Boolean active,
        @JsonProperty(ScimConstants.ENTERPRISE_USER_SCHEMA)
        EnterpriseExtension enterprise,
        @JsonProperty(ScimConstants.ERP_USER_SCHEMA)
        ErpExtension erp
) {

    /*
     신규 projection을 ERP 승인 신청 생성 명령으로 변환한다.

     active가 생략된 SCIM 생성 요청은 활성 원천 계정으로 간주한다.
     그렇더라도 ERP 사용자가 바로 ACTIVE가 되는 것은 아니며,
     application/domain 규칙에 따라 PENDING 승인 대기로 생성된다.
     */
    public CreateProvisionedUserCommand toCreateCommand() {
        return new CreateProvisionedUserCommand(
                resolvedKeycloakSub(),
                enterprise == null ? null : enterprise.employeeNumber(),
                userName,
                resolvedDisplayName(),
                primaryEmail(),
                title,
                resolvedRole(),
                resolvedTenancyType(),
                resolvedTenancyName(),
                active == null || active
        );
    }

    /*
     externalId를 우선 사용한다.

     ExclamationLabs SCIM2 ConnId connector는 User 생성 요청에서 externalId를
     직렬화하지 않으므로, midPoint 연동에서는 userType에도 같은 값을 매핑한다.
     */
    private String resolvedKeycloakSub() {
        return externalId == null || externalId.isBlank()
                ? userType
                : externalId;
    }

    /*
     PUT 요청을 기존 사용자 정보 갱신 명령으로 변환한다.

     userId는 User Service가 발급한 SCIM resource id다.
     externalId 변경 가능 여부는 Controller에서 기존 keycloakSub와 비교해 검증한다.
     */
    public UpdateProvisionedUserCommand toUpdateCommand(Long userId) {
        return new UpdateProvisionedUserCommand(
                userId,
                enterprise == null ? null : enterprise.employeeNumber(),
                userName,
                resolvedDisplayName(),
                primaryEmail(),
                title,
                resolvedRole(),
                resolvedTenancyType(),
                resolvedTenancyName(),
                active
        );
    }

    /*
     displayName을 우선 사용하고, 없으면 SCIM name.formatted를 대체값으로 사용한다.
     */
    private String resolvedDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        return name == null ? null : name.formatted();
    }

    /*
     BBD ERP extension의 tenancyName이 있으면 우선 사용한다.
     없으면 표준 Enterprise extension의 department를 부서/지점명으로 사용한다.
     */
    private String resolvedTenancyName() {
        if (erp != null && erp.tenancyName() != null) {
            return erp.tenancyName();
        }
        return enterprise == null ? null : enterprise.department();
    }

    /*
     ERP extension role을 우선하고, 없으면 표준 roles 배열의 첫 역할을 사용한다.
     SCIM2 ConnId connector가 roles를 누락한 경우에는 nickName을 호환값으로 사용한다.
     */
    private UserRole resolvedRole() {
        if (erp != null && erp.role() != null) {
            return erp.role();
        }
        if (roles != null
                && !roles.isEmpty()
                && roles.getFirst().value() != null) {
            return enumValue(UserRole.class, roles.getFirst().value());
        }
        return nickName == null
                ? null
                : enumValue(UserRole.class, nickName);
    }

    /*
     ERP extension tenancyType을 우선하고,
     없으면 Enterprise organization의 HQ/BRANCH 문자열을 사용한다.
     SCIM2 ConnId connector가 Enterprise extension을 누락한 경우에는 locale을 사용한다.
     */
    private TenancyType resolvedTenancyType() {
        if (erp != null && erp.tenancyType() != null) {
            return erp.tenancyType();
        }
        if (enterprise != null && enterprise.organization() != null) {
            return enumValue(
                    TenancyType.class,
                    enterprise.organization()
            );
        }
        return locale == null
                ? null
                : enumValue(TenancyType.class, locale);
    }

    /*
     외부 문자열을 도메인 enum으로 변환한다.
     지원하지 않는 값은 SCIM invalidValue 오류로 만들어 midPoint에 반환한다.
     */
    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return Enum.valueOf(
                    type,
                    value.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new ScimException(
                    HttpStatus.BAD_REQUEST,
                    "invalidValue",
                    type.getSimpleName() + " 값이 올바르지 않습니다: " + value
            );
        }
    }

    /*
     SCIM emails는 여러 값을 가질 수 있지만 User DB는 대표 이메일 하나를 저장한다.
     primary=true인 값을 우선하고, 없으면 첫 이메일을 사용한다.
     */
    private String primaryEmail() {
        if (emails == null || emails.isEmpty()) {
            return null;
        }

        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.primary()))
                .findFirst()
                .orElse(emails.getFirst())
                .value();
    }

    // SCIM Core User의 name 복합 속성 중 현재 사용하는 formatted 값.
    public record ScimName(String formatted) {
    }

    // SCIM Core User의 multi-valued emails 항목.
    public record ScimEmail(
            String value,
            String type,
            Boolean primary
    ) {
    }

    // SCIM Core User의 multi-valued roles 항목. ERP는 대표 역할 하나를 사용한다.
    public record ScimRole(
            String value,
            String display,
            Boolean primary
    ) {
    }

    // RFC 7643 Enterprise User extension에서 사용하는 사번과 조직 속성.
    public record EnterpriseExtension(
            String employeeNumber,
            String organization,
            String department
    ) {
    }

    // BBD ERP가 추가한 역할과 tenancy 전용 확장 속성.
    public record ErpExtension(
            UserRole role,
            TenancyType tenancyType,
            String tenancyName
    ) {
    }
}