package com.bbd.user.domain;

import java.util.Objects;

/**
 * @param keycloakSub    Keycloak Realm 내 사용자 고유 식별자.
 *                       최종 사용자 식별 기준이다.
 * @param employeeNumber 사번.
 *                       화면 표시, 검색, 업무 식별용으로 사용한다.
 *                       최종 인증/인가 식별 기준은 keycloakSub이다.
 * @param status         우리 서비스 기준 사용자 상태.
 *                       공통 인가 프레임워크는 Redis UserSnapshot에서 이 값을 보고
 *                       ACTIVE 사용자인지 먼저 판단한다.
 * @param role           대표 업무 역할.
 *                       초기 인가는 role 중심으로 시작하고,
 *                       이후 세부 권한은 permission으로 확장할 수 있다.
 * @param tenancyType    사용자의 소속 범위.
 *                       HQ인지 BRANCH인지에 따라 각 MSA의 데이터 접근 범위가 달라질 수 있다.
 * @param tenancyName    화면 표시나 로그에 사용하는 소속 이름.
 *                       인가 판단 기준으로 사용하지 않는다.
 *                       예: "본사", "인천 지점", "강남 지점"
 * @param version        Snapshot 캐시 무효화나 변경 감지를 위한 버전.
 *                       role/status/tenancy가 바뀌면 증가시키는 방향으로 확장할 수 있다.
 */ /*
 우리 서비스의 사용자/인가 관리 도메인 모델.

 이 User는 현대 파츠 원본 DB의 사용자 모델이 아니라,
 우리 BBD ERP 서비스에서 인증/인가 판단을 위해 관리하는 사용자 모델이다.

 이 객체는 나중에 UserSnapshot의 원본이 된다.
 */
public record User(Long id, String keycloakSub, String employeeNumber, String displayName, String email,
                   String position, UserStatus status, UserRole role, TenancyType tenancyType, String tenancyName,
                   Long version) {

    public User(
            Long id,
            String keycloakSub,
            String employeeNumber,
            String displayName,
            String email,
            String position,
            UserStatus status,
            UserRole role,
            TenancyType tenancyType,
            String tenancyName,
            Long version
    ) {
        validateRequired(keycloakSub, "keycloakSub");
        validateRequired(employeeNumber, "employeeNumber");

        this.id = id;
        this.keycloakSub = keycloakSub;
        this.employeeNumber = employeeNumber;
        this.displayName = displayName;
        this.email = email;
        this.position = position;
        this.status = Objects.requireNonNull(status, "status는 필수입니다.");
        this.role = Objects.requireNonNull(role, "role은 필수입니다.");
        this.tenancyType = Objects.requireNonNull(tenancyType, "tenancyType은 필수입니다.");
        this.tenancyName = tenancyName;
        this.version = version == null ? 1L : version;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean belongsToHq() {
        return tenancyType == TenancyType.HQ;
    }

    public boolean belongsToBranch() {
        return tenancyType == TenancyType.BRANCH;
    }

    public boolean hasRole(UserRole requiredRole) {
        return role == requiredRole;
    }

    /*
     midPoint가 ERP 대상자로 판단한 사용자를 승인 대기 상태로 생성한다.

     SCIM의 active는 원천 계정의 활성 여부이고 ERP 사용 승인 여부와는 다르다.
     원천 계정이 활성 상태라면 PENDING으로 생성해 ERP 관리자 승인을 기다리고,
     이미 비활성 상태라면 INACTIVE로 생성한다.
     */
    public static User pendingProvisioning(
            String keycloakSub,
            String employeeNumber,
            String displayName,
            String email,
            String position,
            UserRole role,
            TenancyType tenancyType,
            String tenancyName,
            boolean sourceActive
    ) {
        return new User(
                null,
                keycloakSub,
                employeeNumber,
                displayName,
                email,
                position,
                // 임시 ACTIVE로 처리 (원래는 PENDING)
                sourceActive ? UserStatus.ACTIVE : UserStatus.INACTIVE,
                role,
                tenancyType,
                tenancyName,
                1L
        );
    }

    /*
     SCIM reconciliation에서 전달된 원천 사용자 정보와 ERP 역할 후보를 반영한다.

     active=true는 ERP 승인을 의미하지 않는다.
     기존 ACTIVE 사용자는 ACTIVE를 유지하고, PENDING 사용자는 PENDING을 유지한다.
     퇴사 후 다시 ERP 대상자가 된 INACTIVE 사용자는 재승인을 위해 PENDING으로 전환한다.
     active=false는 즉시 INACTIVE로 전환한다.
     */
    public User updateProvisioning(
            String newEmployeeNumber,
            String newDisplayName,
            String newEmail,
            String newPosition,
            UserRole newRole,
            TenancyType newTenancyType,
            String newTenancyName,
            Boolean sourceActive
    ) {
        UserStatus nextStatus = status;

        if (Boolean.FALSE.equals(sourceActive)) {
            nextStatus = UserStatus.INACTIVE;
        } else if (Boolean.TRUE.equals(sourceActive) && status == UserStatus.INACTIVE) {
            nextStatus = UserStatus.PENDING;
        }

        return new User(
                id,
                keycloakSub,
                valueOrCurrent(newEmployeeNumber, employeeNumber),
                valueOrCurrent(newDisplayName, displayName),
                valueOrCurrent(newEmail, email),
                valueOrCurrent(newPosition, position),
                nextStatus,
                newRole == null ? role : newRole,
                newTenancyType == null ? tenancyType : newTenancyType,
                newTenancyName == null ? tenancyName : newTenancyName,
                version
        );
    }

    /*
     SCIM DELETE와 active=false는 DB row를 지우지 않고 ERP 사용자를 비활성화한다.
     감사 이력과 Keycloak sub 매핑을 유지해야 재입사나 부서 복귀 시 같은 사용자를 찾을 수 있다.
     */
    public User deactivate() {
        return new User(
                id,
                keycloakSub,
                employeeNumber,
                displayName,
                email,
                position,
                UserStatus.INACTIVE,
                role,
                tenancyType,
                tenancyName,
                version
        );
    }

    private String valueOrCurrent(String candidate, String current) {
        return candidate == null ? current : candidate;
    }

    /*
     사용자 인가 판단에 사용하는 상태, 역할, 소속을 변경한다.

     기존 User를 직접 수정하지 않고 새 User를 반환해서
     도메인 모델의 불변성을 유지한다.

     version은 여기서 직접 증가시키지 않는다.
     persistence 계층의 JPA @Version이 DB update 성공 시 증가시키고,
     저장된 Entity를 다시 도메인 모델로 변환해서 최종 version을 반환한다.
     */
    public User changeAuthorization(
            UserStatus newStatus,
            UserRole newRole,
            TenancyType newTenancyType,
            String newTenancyName
    ) {
        return new User(
                id,
                keycloakSub,
                employeeNumber,
                displayName,
                email,
                position,
                Objects.requireNonNull(newStatus, "status는 필수입니다."),
                Objects.requireNonNull(newRole, "role은 필수입니다."),
                Objects.requireNonNull(newTenancyType, "tenancyType은 필수입니다."),
                newTenancyName,
                version
        );
    }
}
