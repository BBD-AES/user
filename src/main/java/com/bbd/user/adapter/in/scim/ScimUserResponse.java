package com.bbd.user.adapter.in.scim;

import com.bbd.user.application.model.UserResult;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/*
 User Service의 application 결과를 RFC 7643 User 응답으로 표현한다.

 id:
 User Service users.id를 SCIM resource id 문자열로 제공한다.

 externalId:
 midPoint가 전달했던 Keycloak sub를 돌려준다.

 schemas:
 Core, Enterprise, BBD ERP extension을 모두 사용한다는 것을 명시한다.

 PENDING은 원천 계정이 비활성이라는 뜻이 아니므로 active=true로 응답한다.
 실제 ERP 승인 상태는 ERP extension의 approvalStatus로 구분한다.

 meta.location:
 이후 midPoint가 GET/PUT/PATCH/DELETE에 사용할 resource URL.

 meta.version:
 JPA version을 weak ETag로 제공해 응답 버전을 식별한다.
 */
public record ScimUserResponse(
        List<String> schemas,
        String id,
        String externalId,
        String userName,
        String displayName,
        ScimUserRequest.ScimName name,
        String title,
        List<ScimUserRequest.ScimEmail> emails,
        List<ScimUserRequest.ScimRole> roles,
        boolean active,
        @JsonProperty(ScimConstants.ENTERPRISE_USER_SCHEMA)
        EnterpriseExtension enterprise,
        @JsonProperty(ScimConstants.ERP_USER_SCHEMA)
        ErpExtension erp,
        Meta meta
) {

    /*
     application 계층의 ProvisionedUserResult를 외부 SCIM 표현으로 변환한다.

     이 변환은 Adapter 내부에서 끝나므로 application/domain 계층은
     SCIM schema, URN, ETag, location 형식을 알 필요가 없다.
     */
    public static ScimUserResponse from(UserResult result, String location) {
        List<ScimUserRequest.ScimEmail> emails = result.email() == null
                ? List.of()
                : List.of(new ScimUserRequest.ScimEmail(result.email(), "work", true));

        return new ScimUserResponse(
                List.of(
                        ScimConstants.CORE_USER_SCHEMA,
                        ScimConstants.ENTERPRISE_USER_SCHEMA,
                        ScimConstants.ERP_USER_SCHEMA
                ),
                result.userId().toString(),
                result.keycloakSub(),
                result.employeeNumber(),
                result.displayName(),
                new ScimUserRequest.ScimName(result.displayName()),
                result.position(),
                emails,
                List.of(new ScimUserRequest.ScimRole(
                        result.role().name(),
                        result.role().name(),
                        true
                )),
                result.status() != UserStatus.INACTIVE,
                new EnterpriseExtension(
                        result.employeeNumber(),
                        result.tenancyType().name(),
                        result.tenancyName()
                ),
                new ErpExtension(
                        result.role(),
                        result.tenancyType(),
                        result.tenancyName(),
                        result.status()
                ),
                new Meta("User", location, "W/\"" + result.version() + "\"")
        );
    }

    // RFC 7643 Enterprise User extension 응답.
    public record EnterpriseExtension(
            String employeeNumber,
            String organization,
            String department
    ) {
    }

    /*
     BBD ERP 전용 응답.

     approvalStatus는 PENDING/ACTIVE/INACTIVE를 그대로 제공해
     SCIM active와 ERP 관리자 승인 상태를 구분한다.
     */
    public record ErpExtension(
            UserRole role,
            TenancyType tenancyType,
            String tenancyName,
            UserStatus approvalStatus
    ) {
    }

    // SCIM resource의 종류, 자기 자신 URL, 현재 버전을 나타낸다.
    public record Meta(
            String resourceType,
            String location,
            String version
    ) {
    }
}