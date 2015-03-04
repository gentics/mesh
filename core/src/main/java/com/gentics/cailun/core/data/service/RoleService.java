package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.response.RestRole;

public interface RoleService extends GenericNodeService<Role> {

	Role findByUUID(String uuid);

	Role findByName(String name);

	RestRole getReponseObject(Role role);

	void addPermission(Role role, User user, PermissionType... permissionTypes);

}
