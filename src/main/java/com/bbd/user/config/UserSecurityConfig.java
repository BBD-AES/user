package com.bbd.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/*
 User Service 전용 Spring Security 설정.

 일반 MSA는 bbd-security-core가 기본 보안 체인을 자동 구성하지만,
 User Service는 앞으로 서로 다른 인증 방식을 사용하는 두 입구를 가진다.

 - /api/**  : Gateway가 전달한 Keycloak Access Token을 검증하는 ERP API
 - /scim/** : midPoint의 client certificate를 검증할 provisioning API

 아직 SCIM mTLS를 구현하지 않았으므로 /scim/**는 전부 차단한다.
 각 SecurityFilterChain은 @Order 순서대로 검사되며,
 먼저 일치한 체인만 해당 요청을 처리한다.
 */
@Configuration
public class UserSecurityConfig {

    /*
     향후 midPoint 전용 mTLS 체인으로 교체할 자리.

     SCIM 구현 전에는 경로가 실수로 외부에 노출되지 않도록 denyAll로 막는다.
     SCIM 단계에서는 이 체인에 x509 인증과 midPoint 인증서 검증을 추가한다.
     */
    @Bean
    @Order(1)
    SecurityFilterChain scimDisabledSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/scim/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .build();
    }

    /*
     ERP 사용자 및 관리자용 API 보안 체인.

     Gateway의 웹 요청은 JSESSIONID로 인증되지만,
     Gateway가 MSA로 라우팅할 때는 세션에 연결된 Access Token을
     Authorization: Bearer 헤더로 Relay한다.

     User Service는 전달받은 JWT의 서명, issuer, 만료 시간을 다시 검증하고
     서버 세션을 만들지 않는 STATELESS Resource Server로 동작한다.
     */
    @Bean
    @Order(2)
    SecurityFilterChain userApiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }

    /*
     인증 없이 열어둘 운영/문서 경로와 최종 차단 규칙.

     앞의 두 체인에 해당하지 않는 요청은 이 체인에서 처리한다.
     health, error, Swagger 문서만 허용하고 나머지는 denyAll로 차단한다.
     */
    @Bean
    @Order(3)
    SecurityFilterChain publicEndpointSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/error",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().denyAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
