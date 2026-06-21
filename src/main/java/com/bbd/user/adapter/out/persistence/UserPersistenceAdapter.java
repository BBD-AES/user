package com.bbd.user.adapter.out.persistence;

import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSearchPage;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/*
 User 조회/수정 persistence adapter.

 application 계층의 LoadUserPort와 SaveUserPort를 구현한다.
 JPA Repository로 DB를 조회한 뒤 User 도메인 모델로 변환해서 반환한다.

 application 계층은 이 adapter를 통해서만 users 테이블에 접근하며,
 JPA Entity나 Repository를 직접 알지 않는다.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return userJpaRepository.findByKeycloakSub(keycloakSub)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmployeeNumber(String employeeNumber) {
        return userJpaRepository.findByEmployeeNumber(employeeNumber)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findAll(int offset, int count) {
        return userJpaRepository.findAllByOffset(offset, count)
                .stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return userJpaRepository.count();
    }

    @Override
    public UserSearchPage searchUsers(UserSearchCondition condition) {
    /*
     검색 조건(Specification)을 적용해 사용자 목록을 페이징 조회한다.
     결과 순서가 흔들리지 않도록 id 오름차순으로 정렬한다.
     */
        Page<UserJpaEntity> result = userJpaRepository.findAll(
                searchSpec(condition),
                PageRequest.of(
                        condition.page(),
                        condition.size(),
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );

    /*
     조회된 JPA Entity 목록을 도메인 User로 변환하고,
     전체 건수와 현재 페이지 정보를 application 계층의 페이지 모델로 반환한다.
     */
        return new UserSearchPage(
                result.getContent().stream()
                        .map(UserJpaEntity::toDomain)
                        .toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Override
    public User save(User user) {
        if (user.id() == null) {
            return saveAndFlush(UserJpaEntity.from(user)).toDomain();
        }

        UserJpaEntity entity = userJpaRepository.findById(user.id())
                .orElseThrow(() -> new IllegalStateException("수정할 사용자가 존재하지 않습니다."));

        entity.updateFrom(user);

        /*
         saveAndFlush를 사용해서 현재 트랜잭션 안에서 DB UPDATE와 @Version 증가를 실행한다.
         반환된 User의 version을 같은 트랜잭션에 저장되는 UserChangedEvent에 사용한다.
         */
        return saveAndFlush(entity).toDomain();
    }

    private UserJpaEntity saveAndFlush(UserJpaEntity entity) {
        try {
            return userJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode = duplicatedUserErrorCode(exception);
            if (errorCode != null) {
                throw new ApiException(errorCode);
            }
            throw exception;
        }
    }

    private ErrorCode duplicatedUserErrorCode(DataIntegrityViolationException exception) {
        String details = duplicateViolationDetails(exception);

        if (!isUniqueViolation(exception, details)) {
            return null;
        }

        if (matchesKeycloakSubUniqueKey(details)) {
            return ErrorCode.USER_DUPLICATED_KEYCLOAK_SUB;
        }
        if (matchesEmployeeNumberUniqueKey(details)) {
            return ErrorCode.USER_DUPLICATED_EMPLOYEE_NUMBER;
        }

        return null;
    }

    private boolean matchesKeycloakSubUniqueKey(String details) {
        return details.contains("users_keycloak_sub_key")
                || details.contains("keycloak_sub_key")
                || details.contains("uk_users_keycloak_sub")
                || details.contains("key (keycloak_sub")
                || details.contains("users(keycloak_sub")
                || details.contains("users (keycloak_sub")
                || details.contains("(keycloak_sub)");
    }

    private boolean matchesEmployeeNumberUniqueKey(String details) {
        return details.contains("users_employee_number_key")
                || details.contains("employee_number_key")
                || details.contains("uk_users_employee_number")
                || details.contains("key (employee_number")
                || details.contains("users(employee_number")
                || details.contains("users (employee_number")
                || details.contains("(employee_number)");
    }

    private String duplicateViolationDetails(DataIntegrityViolationException exception) {
        StringBuilder details = new StringBuilder();
        Throwable current = exception;

        while (current != null) {
            if (current.getMessage() != null) {
                details.append(' ').append(current.getMessage());
            }
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                details.append(' ').append(constraintViolation.getConstraintName());
            }
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                details.append(' ').append(sqlException.getSQLState());
            }
            current = current.getCause();
        }

        return details.toString().toLowerCase(Locale.ROOT);
    }

    private boolean isUniqueViolation(Throwable exception, String details) {
        if (details.contains("23505")
                || details.contains("unique")
                || details.contains("duplicate")) {
            return true;
        }

        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private Specification<UserJpaEntity> searchSpec(UserSearchCondition condition) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condition.employeeNumber() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("employeeNumber")),
                        contains(condition.employeeNumber())
                ));
            }

            if (condition.name() != null) {
                String name = contains(condition.name());
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("displayName")),
                                name
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("employeeNumber")),
                                name
                        )
                ));
            }

            UserRole role = condition.role();
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            TenancyType tenancyType = condition.tenancyType();
            if (tenancyType != null) {
                predicates.add(criteriaBuilder.equal(root.get("tenancyType"), tenancyType));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String contains(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}
