package com.bbd.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 @ConfigurationPropertiesScan:
 application.yml의 bbd.user.events.*, bbd.user.scim.* 값을
 UserEventProperties, ScimProperties 같은 설정 객체에 바인딩한다.

 @EnableScheduling:
 user_outbox 테이블의 PENDING 이벤트를 주기적으로 조회해서
 Kafka로 발행하는 UserOutboxPublisher와
 snapshot_invalidation_outbox의 Redis 삭제 retry scheduler를 실행한다.

 Kafka/Redis 연동을 사용하지 않는 환경에서는
 bbd.user.events.enabled=false로 관련 Bean 자체를 만들지 않는다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class UserApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}

}
