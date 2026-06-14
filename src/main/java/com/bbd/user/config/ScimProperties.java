package com.bbd.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 midPoint SCIM 연결 설정.

 enabled=false가 기본값이므로 인증서와 HTTPS 설정이 준비되기 전에는
 /scim/** 경로가 기존 Phase 6과 동일하게 전부 차단된다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bbd.user.scim")
public class ScimProperties {

    private boolean enabled;
    private String allowedClientCommonName = "midpoint";
}
