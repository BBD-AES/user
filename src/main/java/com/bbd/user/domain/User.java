package com.bbd.user.domain;

import lombok.Getter;

import java.util.Objects;

/*
 우리 서비스의 사용자/인가 관리 도메인 모델.

 이 User는 현대 파츠 원본 DB의 사용자 모델이 아니라,
 우리 BBD ERP 서비스에서 인증/인가 판단을 위해 관리하는 사용자 모델이다.

 이 객체는 나중에 UserSnapshot의 원본이 된다.
 */
@Getter
public class User {

    private final Long id;

    /*
     Keycloak Realm 내 사용자 고유 식별자.
     최종 사용자 식별 기준이다.
     */
    private final String keycloakSub;

    /*
     사번.
     화면 표시, 검색, 업무 식별용으로 사용한다.
     최종 인증/인가 식별 기준은 keycloakSub이다.
     */
    private final String employeeNumber;

    private final String username;
    private final String displayName;
    private final String email;
    private final String position;

    /*
     우리 서비스 기준 사용자 상태.
     공통 인가 프레임워크는 Redis UserSnapshot에서 이 값을 보고
     ACTIVE 사용자인지 먼저 판단한다.
     */
    private final UserStatus status;

    /*
     대표 업무 역할.
     초기 인가는 role 중심으로 시작하고,
     이후 세부 권한은 permission으로 확장할 수 있다.
     */
    private final UserRole role;

    /*
     사용자의 소속 범위.
     HQ인지 BRANCH인지에 따라 각 MSA의 데이터 접근 범위가 달라질 수 있다.
     */
    private final TenancyType tenancyType;

    /*
     인가 판단에 사용하는 소속 식별자.
     HQ면 본사 ID, BRANCH면 지점 ID처럼 사용한다.
     예: 인천 지점이라면 tenancyId = 3
     */
    private final Long tenancyId;

    /*
     화면 표시나 로그에 사용하는 소속 이름.
     인가 판단 기준으로 사용하지 않는다.
     예: "본사", "인천 지점", "강남 지점"
     */
    private final String tenancyName;

    /*
     Snapshot 캐시 무효화나 변경 감지를 위한 버전.
     role/status/tenancy가 바뀌면 증가시키는 방향으로 확장할 수 있다.
     */
    private final Long version;

    public User(
            Long id,
            String keycloakSub,
            String employeeNumber,
            String username,
            String displayName,
            String email,
            String position,
            UserStatus status,
            UserRole role,
            TenancyType tenancyType,
            Long tenancyId,
            String tenancyName,
            Long version
    ) {
        validateRequired(keycloakSub, "keycloakSub");
        validateRequired(employeeNumber, "employeeNumber");

        this.id = id;
        this.keycloakSub = keycloakSub;
        this.employeeNumber = employeeNumber;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.position = position;
        this.status = Objects.requireNonNull(status, "status는 필수입니다.");
        this.role = Objects.requireNonNull(role, "role은 필수입니다.");
        this.tenancyType = Objects.requireNonNull(tenancyType, "tenancyType은 필수입니다.");
        this.tenancyId = tenancyId;
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

    public boolean belongsToTenancy(Long targetTenancyId) {
        return tenancyId != null && tenancyId.equals(targetTenancyId);
    }
}