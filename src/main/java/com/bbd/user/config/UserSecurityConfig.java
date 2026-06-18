package com.bbd.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/*
 User Service 전용 Spring Security 설정.

 일반 MSA는 bbd-security-core가 기본 보안 체인을 자동 구성하지만,
 User Service는 앞으로 서로 다른 인증 방식을 사용하는 두 입구를 가진다.

 - /api/**  : Gateway 또는 MSA가 전달한 Keycloak Access Token을 검증하는 ERP API
 - /scim/** : admin-be client credentials JWT 또는 midPoint client certificate를
              검증하는 provisioning API

 SCIM 기능을 끈 환경에서는 /scim/**를 전부 차단한다.
 기능을 켠 환경에서는 Bearer JWT와 X.509 인증을 모두 지원하고,
 둘 중 하나가 ROLE_SCIM_CLIENT 권한을 만들면 요청을 허용한다.
 각 SecurityFilterChain은 @Order 순서대로 검사되며,
 먼저 일치한 체인만 해당 요청을 처리한다.
 */
@Configuration
public class UserSecurityConfig {

    @Bean
    @Order(1)
    @ConditionalOnProperty(
            prefix = "bbd.user.scim",
            name = "enabled",
            havingValue = "true"
    )
    SecurityFilterChain scimSecurityFilterChain(
            HttpSecurity http,
            ScimProperties properties
    ) throws Exception {
        String requiredAuthority = roleAuthority(properties.getRequiredRole());
        UserDetailsService midPointClient = commonName -> {
            if (!properties.getAllowedClientCommonName().equals(commonName)) {
                throw new UsernameNotFoundException(
                        "허용되지 않은 SCIM client certificate CN입니다."
                );
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(commonName)
                    .password("")
                    .authorities(requiredAuthority)
                    .build();
        };

        return http
                .securityMatcher("/scim/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAuthority(requiredAuthority)
                )
                .x509(x509 -> x509
                        .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                        .userDetailsService(midPointClient)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                        )
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .build();
    }

    /*
     SCIM 기능을 명시적으로 켜지 않은 환경에서는 SCIM 경로를 전부 차단한다.
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(
            prefix = "bbd.user.scim",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
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

    private Converter<Jwt, Collection<GrantedAuthority>> keycloakRoleAuthoritiesConverter() {
        return jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();
            addRoles(authorities, jwt.getClaim("realm_access"));
            addResourceRoles(authorities, jwt.getClaim("resource_access"));
            return authorities;
        };
    }

    private JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakRoleAuthoritiesConverter());
        return converter;
    }

    private void addRoles(Set<GrantedAuthority> authorities, Object access) {
        if (access instanceof Map<?, ?> accessMap) {
            Object roles = accessMap.get("roles");
            if (roles instanceof Iterable<?> iterable) {
                iterable.forEach(role -> {
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority(roleAuthority(role.toString())));
                    }
                });
            }
        }
    }

    private void addResourceRoles(Set<GrantedAuthority> authorities, Object resourceAccess) {
        if (resourceAccess instanceof Map<?, ?> resourceMap) {
            resourceMap.values().forEach(access -> addRoles(authorities, access));
        }
    }

    private String roleAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_SCIM_CLIENT";
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
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
