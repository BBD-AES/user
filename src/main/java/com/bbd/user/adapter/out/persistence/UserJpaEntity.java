package com.bbd.user.adapter.out.persistence;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/*
 users 테이블과 매핑되는 JPA Entity.

 이 객체는 도메인 모델이 아니라 persistence adapter의 DB 매핑 모델이다.
 application/domain 계층에서는 이 타입을 직접 사용하지 않는다.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     Keycloak Realm 내 사용자 고유 식별자.
     최종 인증/인가 식별 기준이다.
     */
    @Column(name = "keycloak_sub", nullable = false, unique = true, length = 100)
    private String keycloakSub;

    /*
     사번.
     화면 표시, 검색, 업무 식별용이다.
     */
    @Column(name = "employee_number", nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "position", length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenancy_type", nullable = false, length = 30)
    private TenancyType tenancyType;

    @Column(name = "tenancy_name", length = 100)
    private String tenancyName;

    /*
     동시에 두 요청이 같은 사용자를 수정할 때 마지막 요청이 앞선 변경을 덮어쓰는 문제를 막는다.

     JPA는 UPDATE 시 기존 version을 WHERE 조건에 포함하고,
     성공하면 version을 자동으로 1 증가시킨다.
     이미 다른 트랜잭션이 수정했다면 OptimisticLockException이 발생한다.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // 최초 사용자 row가 만들어진 시각. update에서는 변경하지 않는다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // 관리자 API 또는 SCIM 변경이 마지막으로 반영된 시각.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /*
     JPA Entity를 순수 도메인 User로 변환한다.
     application 계층에는 JPA Entity가 아니라 User 도메인 모델을 전달한다.
     */
    public User toDomain() {
        return new User(
                id,
                keycloakSub,
                employeeNumber,
                displayName,
                email,
                position,
                status,
                role,
                tenancyType,
                tenancyName,
                version
        );
    }

    /*
     SCIM POST로 생성된 신규 User 도메인을 users 테이블 INSERT용 Entity로 변환한다.
     */
    public static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        Instant now = Instant.now();

        entity.keycloakSub = user.keycloakSub();
        entity.employeeNumber = user.employeeNumber();
        entity.displayName = user.displayName();
        entity.email = user.email();
        entity.position = user.position();
        entity.status = user.status();
        entity.role = user.role();
        entity.tenancyType = user.tenancyType();
        entity.tenancyName = user.tenancyName();
        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    /*
     application 계층에서 변경한 User 도메인 값을 영속 Entity에 반영한다.

     id, createdAt, version은 JPA가 관리하므로 직접 덮어쓰지 않는다.
     특히 version은 @Version이 update 성공 시 자동 증가시킨다.
     */
    public void updateFrom(User user) {
        this.keycloakSub = user.keycloakSub();
        this.employeeNumber = user.employeeNumber();
        this.displayName = user.displayName();
        this.email = user.email();
        this.position = user.position();
        this.status = user.status();
        this.role = user.role();
        this.tenancyType = user.tenancyType();
        this.tenancyName = user.tenancyName();
        this.updatedAt = Instant.now();
    }
}
