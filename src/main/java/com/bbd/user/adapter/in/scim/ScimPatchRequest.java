package com.bbd.user.adapter.in.scim;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/*
 RFC 7644 PATCH 요청.

 midPoint가 주로 사용하는 add, replace, remove operation을 지원한다.

 schemas:
 요청이 SCIM PatchOp 문서임을 나타내는 URN 목록.

 Operations:
 RFC 7644에서 대문자 O를 사용하는 JSON 필드이므로 @JsonProperty로 매핑한다.

 각 Operation은 다음 값을 가진다.
 - op: add, replace, remove
 - path: 변경할 SCIM 속성 경로
 - value: 문자열, boolean, 객체, 배열 중 하나

 value는 속성마다 JSON 형태가 다르므로 여기서는 Object로 수신한다.
 실제 형식 검증과 application command 변환은 ScimPatchMapper가 담당한다.
 */
public record ScimPatchRequest(
        List<String> schemas,
        @JsonProperty("Operations")
        List<Operation> operations
) {

    public record Operation(
            String op,
            String path,
            Object value
    ) {
        /*
         path가 없는 add/replace operation은 value 전체가 속성 Map으로 전달된다.

         Mapper가 해당 형식을 처리할 수 있도록 Map으로 변환하고,
         객체 형태가 아니면 빈 Map을 반환해 최종적으로 지원되지 않는 요청으로 처리한다.
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> valueAsMap() {
            return value instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Map.of();
        }
    }
}
