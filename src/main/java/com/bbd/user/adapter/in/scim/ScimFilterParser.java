package com.bbd.user.adapter.in.scim;

import com.bbd.user.application.model.ProvisionedUserSearchField;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 midPoint reconciliation에 필요한 exact-match SCIM filter를 파싱한다.

 midPoint는 자신이 관리하는 projection과 User Service의 실제 사용자를 비교할 때
 GET /Users?filter=... 형태로 기존 계정을 검색한다.
 이 클래스는 외부 SCIM filter 문자열을 application 계층의 검색 필드로 번역한다.

 현재 지원:
 - externalId eq "Keycloak sub"
 - userName eq "login id"
 - urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber eq "EMP001"

 임의 SQL 조건이나 복합 filter를 허용하지 않고 위 exact-match만 허용한다.
 지원하지 않는 문법 또는 속성은 ScimException(invalidFilter)으로 변환되어
 ScimExceptionHandler를 거쳐 midPoint에 SCIM Error 응답으로 전달된다.
 */
@Component
public class ScimFilterParser {

    private static final Pattern EQUALS_FILTER = Pattern.compile(
            "^\\s*([A-Za-z0-9:._-]+)\\s+eq\\s+\"([^\"]*)\"\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    /*
     filter가 없으면 전체 목록 조회를 의미하므로 field와 value가 없는 결과를 반환한다.

     filter가 있으면 SCIM 속성 이름을 User Service 검색 기준으로 바꾼다.
     예를 들어 externalId는 DB의 keycloak_sub 검색으로 연결된다.
     */
    public ParsedFilter parse(String filter) {
        if (filter == null || filter.isBlank()) {
            return new ParsedFilter(null, null);
        }

        Matcher matcher = EQUALS_FILTER.matcher(filter);
        if (!matcher.matches()) {
            throw invalidFilter(filter);
        }

        String attribute = matcher.group(1);
        String value = matcher.group(2);

        if ("externalId".equalsIgnoreCase(attribute)) {
            return new ParsedFilter(ProvisionedUserSearchField.KEYCLOAK_SUB, value);
        }
        if ("userName".equalsIgnoreCase(attribute)) {
            return new ParsedFilter(ProvisionedUserSearchField.USERNAME, value);
        }
        if ((ScimConstants.ENTERPRISE_USER_SCHEMA + ":employeeNumber")
                .equalsIgnoreCase(attribute)) {
            return new ParsedFilter(ProvisionedUserSearchField.EMPLOYEE_NUMBER, value);
        }

        throw invalidFilter(filter);
    }

    private ScimException invalidFilter(String filter) {
        return new ScimException(
                HttpStatus.BAD_REQUEST,
                "invalidFilter",
                "지원하지 않는 SCIM filter입니다: " + filter
        );
    }

    /*
     Controller와 application command 사이에서 사용하는 Adapter 내부 파싱 결과.

     SCIM 원문을 application 계층까지 넘기지 않고
     이미 해석된 검색 필드와 검색값만 전달하기 위한 값 객체다.
     */
    public record ParsedFilter(
            ProvisionedUserSearchField field,
            String value
    ) {
    }
}
