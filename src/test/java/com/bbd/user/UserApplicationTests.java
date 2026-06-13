package com.bbd.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/*
 User Service 전체 Spring Context가 정상적으로 생성되는지 확인하는 smoke test.

 외부 PostgreSQL, Kafka, Redis, Keycloak 없이 실행할 수 있도록:

 - H2를 PostgreSQL 호환 모드로 사용
 - Flyway migration 실행
 - JWT decoder는 실제 issuer 조회 대신 test JWK URI 사용
 - Kafka/Redis event Bean은 enabled=false로 비활성화
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:user;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.flyway.enabled=true",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/test-jwks",
		"bbd.user.events.enabled=false"
})
class UserApplicationTests {

	@Test
	void contextLoads() {
		// Context 생성 자체가 검증 대상이다.
	}

}
