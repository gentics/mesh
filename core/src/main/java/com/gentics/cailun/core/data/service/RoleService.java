package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.response.RestRole;

public interface RoleService extends GenericNodeService<Role> {

	Role findByUUID(String uuid);

	Role findByName(String name);

	RestRole getReponseObject(Role role);

	void addPermission(Role role, GenericNode node, PermissionType... permissionTypes);

	/**
	 * Return the graph permission between the role and the given node.
	 * 
	 * @param role
	 * @param node
	 * @return found permission or null when no permission could be found
	 */
	GraphPermission getGraphPermission(Role role, GenericNode node);

	GraphPermission revokePermission(Role role, GenericNode node, PermissionType... permissionTypes);

}
