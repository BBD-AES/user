package com.bbd.user.application.port.in;

import com.bbd.user.application.model.*;

/*
 midPoint SCIM adapter가 사용하는 사용자 provisioning inbound port.
 */
public interface ManageProvisionedUserUseCase {

    ProvisionedUserResult create(CreateProvisionedUserCommand command);

    ProvisionedUserResult update(UpdateProvisionedUserCommand command);

    ProvisionedUserResult deactivate(Long userId);

    ProvisionedUserResult getById(Long userId);

    ProvisionedUserSearchResult search(SearchProvisionedUsersCommand command);
}
