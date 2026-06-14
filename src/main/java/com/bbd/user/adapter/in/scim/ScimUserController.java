package com.bbd.user.adapter.in.scim;

import com.bbd.user.application.model.ProvisionedUserResult;
import com.bbd.user.application.model.ProvisionedUserSearchResult;
import com.bbd.user.application.model.SearchProvisionedUsersCommand;
import com.bbd.user.application.port.in.ManageProvisionedUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/*
 midPoint가 호출하는 SCIM 2.0 User Inbound Adapter.

 ERP 대상자 선택 자체는 midPoint의 role/assignment 정책이 담당한다.
 User Service는 호출된 사용자의 승인 신청 생성, 정보 변경, 비활성화를 담당한다.

 역할:
 1. SCIM HTTP 요청과 query parameter를 수신한다.
 2. SCIM DTO/filter/PATCH를 application command로 변환한다.
 3. ManageProvisionedUserUseCase를 호출한다.
 4. application 결과를 SCIM User/ListResponse와 ETag로 변환한다.

 Controller는 JPA Repository나 Redis/Kafka를 직접 호출하지 않는다.
 User DB 변경과 Outbox 저장은 application service가 같은 transaction에서 수행하고,
 commit 이후 Snapshot 삭제와 Kafka 복구 흐름은 Phase 6 구성 요소가 담당한다.

 SCIM 요청 중 발생한 ScimException과 ApiException은
 ScimExceptionHandler가 RFC 7644 Error 응답으로 변환하며,
 최종적으로 midPoint SCIM2 Connector가 그 HTTP 응답을 받는다.
 */
@RestController
@RequestMapping(
        value = "/scim/v2/Users",
        produces = {ScimConstants.MEDIA_TYPE, APPLICATION_JSON_VALUE}
)
@RequiredArgsConstructor
public class ScimUserController {

    private final ManageProvisionedUserUseCase manageProvisionedUserUseCase;
    private final ScimFilterParser filterParser;
    private final ScimPatchMapper patchMapper;

    /*
     ERP 대상 사용자 projection을 최초 생성한다.

     성공 시:
     - HTTP 201 Created
     - Location: 생성된 /scim/v2/Users/{id}
     - ETag: 현재 User version
     - body: 생성된 SCIM User

     같은 externalId, 사번, username이 이미 존재하면 application의 ApiException이 발생하고
     SCIM Handler가 409 uniqueness 응답으로 변환한다.
     */
    @PostMapping(consumes = {ScimConstants.MEDIA_TYPE, "application/json"})
    public ResponseEntity<ScimUserResponse> create(@RequestBody ScimUserRequest request) {
        ProvisionedUserResult created =
                manageProvisionedUserUseCase.create(request.toCreateCommand());
        ScimUserResponse response = response(created);

        return ResponseEntity
                .created(URI.create(response.meta().location()))
                .header(HttpHeaders.ETAG, response.meta().version())
                .body(response);
    }

    /*
     User Service가 발급한 SCIM resource id(users.id)로 사용자 하나를 조회한다.
     존재하지 않으면 application의 USER_NOT_FOUND가 SCIM 404 응답으로 변환된다.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ScimUserResponse> get(@PathVariable Long userId) {
        ScimUserResponse response =
                response(manageProvisionedUserUseCase.getById(userId));

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, response.meta().version())
                .body(response);
    }

    /*
     midPoint의 reconciliation 및 전체 import를 위한 목록 조회다.

     filter가 없으면 startIndex/count 기반 목록을 반환하고,
     filter가 있으면 ScimFilterParser가 허용된 exact-match 검색으로 제한한다.
     SCIM startIndex는 0이 아니라 1부터 시작한다.
     */
    @GetMapping
    public ScimListResponse<ScimUserResponse> search(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "100") int count
    ) {
        ScimFilterParser.ParsedFilter parsed = filterParser.parse(filter);
        ProvisionedUserSearchResult result = manageProvisionedUserUseCase.search(
                new SearchProvisionedUsersCommand(
                        parsed.field(),
                        parsed.value(),
                        startIndex,
                        count
                )
        );

        List<ScimUserResponse> resources = result.users()
                .stream()
                .map(this::response)
                .toList();

        return ScimListResponse.of(
                resources,
                result.totalResults(),
                result.startIndex()
        );
    }

    /*
     기존 SCIM User의 전체 표현을 갱신한다.

     externalId는 Keycloak sub이므로 다른 값으로 교체할 수 없다.
     동일한 값 또는 생략은 허용하고, 변경 시도는 mutability 오류로 거절한다.
     */
    @PutMapping(
            value = "/{userId}",
            consumes = {ScimConstants.MEDIA_TYPE, "application/json"}
    )
    public ResponseEntity<ScimUserResponse> replace(
            @PathVariable Long userId,
            @RequestBody ScimUserRequest request
    ) {
        ProvisionedUserResult current = manageProvisionedUserUseCase.getById(userId);
        if (request.externalId() != null
                && !request.externalId().equals(current.keycloakSub())) {
            throw new ScimException(
                    HttpStatus.BAD_REQUEST,
                    "mutability",
                    "externalId(Keycloak sub)는 생성 후 변경할 수 없습니다."
            );
        }

        ScimUserResponse response = response(
                manageProvisionedUserUseCase.update(request.toUpdateCommand(userId))
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, response.meta().version())
                .body(response);
    }

    /*
     midPoint가 보낸 부분 변경 operation을 적용한다.

     ScimPatchMapper가 SCIM path와 다양한 JSON value 형태를 해석하고,
     application 계층에는 UpdateProvisionedUserCommand만 전달한다.
     */
    @PatchMapping(
            value = "/{userId}",
            consumes = {ScimConstants.MEDIA_TYPE, "application/json"}
    )
    public ResponseEntity<ScimUserResponse> patch(
            @PathVariable Long userId,
            @RequestBody ScimPatchRequest request
    ) {
        ScimUserResponse response = response(
                manageProvisionedUserUseCase.update(
                        patchMapper.toCommand(userId, request)
                )
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, response.meta().version())
                .body(response);
    }

    /*
     SCIM DELETE는 users row를 실제 삭제하지 않는다.
     ERP 접근을 즉시 차단하고 재입사/부서 복귀 시 같은 계정을 다시 연결할 수 있게 INACTIVE로 남긴다.

     성공 시 body 없이 204 No Content를 반환한다.
     application service는 USER_DEACTIVATED Outbox 이벤트도 함께 저장한다.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        manageProvisionedUserUseCase.deactivate(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /*
     SCIM resource의 절대 location을 만들고 application 결과를 응답 DTO로 변환한다.
     현재 요청의 scheme, host, context path를 사용하므로 로컬과 배포 환경에서 같은 코드를 사용한다.
     */
    private ScimUserResponse response(ProvisionedUserResult result) {
        String location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/scim/v2/Users/{id}")
                .buildAndExpand(result.userId())
                .toUriString();

        return ScimUserResponse.from(result, location);
    }
}
