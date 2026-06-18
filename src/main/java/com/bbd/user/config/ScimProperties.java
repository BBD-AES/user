package com.bbd.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 SCIM 연결 설정.

 enabled=false가 기본값이므로 /scim/** 경로는 명시적으로 열기 전까지 전부 차단된다.
 enabled=true인 환경에서는 admin-be의 client credentials JWT와
 향후 midPoint의 X.509 client certificate를 같은 SCIM 권한으로 허용한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bbd.user.scim")
public class ScimProperties {

    private boolean enabled;
    private String allowedClientCommonName = "midpoint";
    private String requiredRole = "SCIM_CLIENT";
}