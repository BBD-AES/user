package com.bbd.user.adapter.out.persistence;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "username", length = 100)
    private String username;

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

    @Column(name = "tenancy_id")
    private Long tenancyId;

    @Column(name = "tenancy_name", length = 100)
    private String tenancyName;

    @Column(name = "version", nullable = false)
    private Long version;

    /*
     JPA Entity를 순수 도메인 User로 변환한다.
     application 계층에는 JPA Entity가 아니라 User 도메인 모델을 전달한다.
     */
    public User toDomain() {
        return new User(
                id,
                keycloakSub,
                employeeNumber,
                username,
                displayName,
                email,
                position,
                status,
                role,
                tenancyType,
                tenancyId,
                tenancyName,
                version
        );
    }
}