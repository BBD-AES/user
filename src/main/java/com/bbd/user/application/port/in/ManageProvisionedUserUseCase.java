package com.bbd.user.application.port.in;

import com.bbd.user.application.model.*;

/*
 midPoint SCIM adapter가 사용하는 사용자 provisioning inbound port.
 */
public interface ManageProvisionedUserUseCase {

    UserResult create(CreateProvisionedUserCommand command);

    UserResult update(UpdateProvisionedUserCommand command);

    UserResult deactivate(Long userId);

    UserResult getById(Long userId);

    ProvisionedUserSearchResult search(SearchProvisionedUsersCommand command);
}
