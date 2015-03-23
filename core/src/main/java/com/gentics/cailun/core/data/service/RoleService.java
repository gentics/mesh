package com.gentics.cailun.core.data.service;

import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.role.response.RoleResponse;

public interface RoleService extends GenericNodeService<Role> {

	Role findByUUID(String uuid);

	Role findByName(String name);

	Result<Role> findAll();

	void addPermission(Role role, AbstractPersistable node, PermissionType... permissionTypes);

	/**
	 * Return the graph permission between the role and the given node.
	 * 
	 * @param role
	 * @param node
	 * @return found permission or null when no permission could be found
	 */
	GraphPermission getGraphPermission(Role role, AbstractPersistable node);

	GraphPermission revokePermission(Role role, AbstractPersistable node, PermissionType... permissionTypes);

	RoleResponse transformToRest(Role role);

}
