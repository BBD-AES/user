package com.bbd.user.adapter.out.persistence;

import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSearchPage;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

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
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findAll(int offset, int count) {
        int page = offset / count;

        return userJpaRepository.findAll(
                        PageRequest.of(page, count, Sort.by(Sort.Direction.ASC, "id"))
                )
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
        Page<UserJpaEntity> result = userJpaRepository.findAll(
                searchSpec(condition),
                PageRequest.of(
                        condition.page(),
                        condition.size(),
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );

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
        if (user.getId() == null) {
            return userJpaRepository.saveAndFlush(UserJpaEntity.from(user)).toDomain();
        }

        UserJpaEntity entity = userJpaRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("수정할 사용자가 존재하지 않습니다."));

        entity.updateFrom(user);

        /*
         saveAndFlush를 사용해서 현재 트랜잭션 안에서 DB UPDATE와 @Version 증가를 실행한다.
         반환된 User의 version을 같은 트랜잭션에 저장되는 UserChangedEvent에 사용한다.
         */
        return userJpaRepository.saveAndFlush(entity).toDomain();
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
                                criteriaBuilder.lower(root.get("username")),
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
