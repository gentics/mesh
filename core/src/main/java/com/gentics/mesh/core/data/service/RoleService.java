package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.Result;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface RoleService extends GenericNodeService<Role> {

	Role findByUUID(String uuid);

	Role findByName(String name);

	Result<Role> findAll();

	void addPermission(Role role, MeshVertex node, PermissionType... permissionTypes);

	/**
	 * Return the graph permission between the role and the given node.
	 * 
	 * @param role
	 * @param node
	 * @return found permission or null when no permission could be found
	 */
	GraphPermission getGraphPermission(Role role, MeshVertex node);

	GraphPermission revokePermission(Role role, MeshVertex node, PermissionType... permissionTypes);

	RoleResponse transformToRest(Role role);

	void addCRUDPermissionOnRole(RoutingContext rc, MeshPermission meshPermission, GenericNode targetNode);

	Page<Role> findAll(RoutingContext rc, PagingInfo pagingInfo);

	Page<Role> findByGroup(RoutingContext rc, Group group, PagingInfo pagingInfo);

	Role create(String name);

	RoleRoot createRoot();

	RoleRoot  findRoot();


}
