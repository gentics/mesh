package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.path.PagingInfo;

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

	void addCRUDPermissionOnRole(RoutingContext rc, CaiLunPermission caiLunPermission, GenericNode targetNode);

	Page<Role> findAllVisible(User requestUser, PagingInfo pagingInfo);

}
