package com.bbd.user.application.model;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;

/*
 midPoint가 SCIM POST로 전달한 ERP 대상 사용자 생성 명령.

 keycloakSub는 SCIM externalId와 매핑한다.
 employeeNumber는 Enterprise User extension의 employeeNumber와 매핑한다.
 sourceActive는 원천 계정의 활성 상태이며 ERP 승인 상태와는 별개다.
 */
public record CreateProvisionedUserCommand(
        String keycloakSub,
        String employeeNumber,
        String username,
        String displayName,
        String email,
        String position,
        UserRole role,
        TenancyType tenancyType,
        String tenancyName,
        boolean sourceActive
) {
}
