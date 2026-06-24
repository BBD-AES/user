package com.bbd.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/*
 SCIM_ENABLED=true일 때 X.509 전용 SecurityFilterChain이 정상 생성되는지 확인한다.

 실제 인증서 handshake는 E2E 단계에서 검증하고,
 이 테스트는 Spring Security 설정과 Bean 구성이 깨지지 않는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scim-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/test-jwks",
        "bbd.user.events.enabled=false",
        "bbd.user.events.snapshot-invalidation-enabled=false",
        "bbd.security.enabled=false",
        "bbd.user.scim.enabled=true",
        "bbd.user.scim.allowed-client-common-name=midpoint"
})
class ScimSecurityContextTest {

    @Test
    void contextLoadsWithMtlsSecurityChain() {
        // SCIM X.509 SecurityFilterChain 생성 자체가 검증 대상이다.
    }
}
