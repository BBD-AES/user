package com.bbd.user.adapter.out.securitycore;

import com.bbd.securitycore.application.port.out.LoadUserSnapshotPort;
import com.bbd.securitycore.domain.UserSnapshot;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityCoreUserSnapshotAdapter implements LoadUserSnapshotPort {

    private final LoadUserPort loadUserPort;

    @Override
    public UserSnapshot loadByKeycloakSub(String keycloakSub) {
        return loadUserPort.findByKeycloakSub(keycloakSub)
                .map(this::toSnapshot)
                .orElse(null);
    }

    private UserSnapshot toSnapshot(User user) {
        return new UserSnapshot(
                user.id(),
                user.keycloakSub(),
                user.employeeNumber(),
                user.displayName(),
                user.email(),
                user.position(),
                com.bbd.securitycore.domain.UserStatus.valueOf(user.status().name()),
                com.bbd.securitycore.domain.UserRole.valueOf(user.role().name()),
                com.bbd.securitycore.domain.TenancyType.valueOf(user.tenancyType().name()),
                user.tenancyName(),
                user.version()
        );
    }
}