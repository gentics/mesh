package com.gentics.mesh.core.data.dao;

import java.util.Set;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import io.vertx.core.json.JsonObject;

public interface RoleDaoWrapper extends RoleDao, DaoWrapper<Role>, DaoTransformable<Role,RoleResponse> {

	/**
	 * Create a new role with the given name.
	 * 
	 * @param name
	 *            Name of the new role.
	 * @param creator
	 *            User that is being used to set the reference fields
	 * @return Created role
	 */
	default Role create(String name, User creator) {
		return create(name, creator, null);
	}

	/**
	 * Create a new role with the given name.
	 * 
	 * @param name
	 *            Name of the new role.
	 * @param creator
	 *            User that is being used to set the reference fields
	 * @param uuid
	 *            Optional uuid
	 * @return Created role
	 */
	Role create(String name, User creator, String uuid);

	Role create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Grant the given permissions on the vertex.
	 *
	 * @param role
	 * @param vertex
	 * @param permissions
	 */
	void grantPermissions(Role role, MeshVertex vertex, GraphPermission... permissions);

	/**
	 * Revoke the given permissions on the vertex.
	 *
	 * @param role
	 * @param vertex
	 * @param permissions
	 */
	void revokePermissions(Role role, MeshVertex vertex, GraphPermission... permissions);

	/**
	 * Return a set of permissions which the role is granting to the given element.
	 *
	 * @param role
	 * @param element
	 * @return Set of permissions of the element
	 */
	Set<GraphPermission> getPermissions(Role role, MeshVertex element);

	/**
	 * Add the given role to this aggregation vertex.
	 * 
	 * @param role
	 *            Role to be added
	 */
	void addRole(Role role);

	/**
	 * Remove the given role from this aggregation vertex.
	 * 
	 * @param role
	 *            Role to be removed
	 */
	void removeRole(Role role);

	/**
	 * Return a page of groups to which this role was assigned.
	 *
	 * @param role
	 * @param user
	 * @param params
	 * @return Loaded page
	 */
	Page<? extends Group> getGroups(Role role, User user, PagingParameters params);

	/**
	 * Check whether the role grants the given permission on the given element.
	 *
	 * @param role
	 * @param permission
	 * @param element
	 * @return
	 */
	boolean hasPermission(Role role, GraphPermission permission, MeshVertex element);

	Role findByUuid(String uuid);

	Role findByName(String roleName);

	void delete(Role role, BulkActionContext bac);

	Role loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm);

	Role loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	boolean update(Role role, InternalActionContext ac, EventQueueBatch batch);

	TraversalResult<? extends Role> findAll();

	TransformablePage<? extends Role> findAll(InternalActionContext ac, PagingParameters pagingInfo);
	
}
