package com.bbd.user.adapter.in.scim;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 SCIM2 Connector 1.2.9 호환을 위한 read-only Group endpoint.

 해당 Connector는 실제 Group provisioning 사용 여부와 무관하게 연결 검사에서
 User와 Group ResourceType을 모두 요구한다. User Service의 ERP 권한은 User.roles와
 ERP extension으로 관리하므로 별도 Group 데이터를 저장하거나 변경하지 않는다.

 midPoint가 Group 목록을 조회하면 빈 SCIM ListResponse를 반환한다.
 생성, 변경, 삭제 endpoint는 제공하지 않아 Group projection을 User Service에
 프로비저닝하는 잘못된 구성을 조기에 드러낸다.
 */
@Tag(name = "2. SCIM Group Controller")
@RestController
@RequestMapping(
        value = "/scim/v2/Groups",
        produces = {ScimConstants.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE}
)
public class ScimGroupController {

    @Operation(summary = "SCIM 그룹 목록 조회 API")
    @GetMapping
    public ScimListResponse<Map<String, Object>> findAll() {
        return ScimListResponse.of(java.util.List.of(), 0, 1);
    }
}
