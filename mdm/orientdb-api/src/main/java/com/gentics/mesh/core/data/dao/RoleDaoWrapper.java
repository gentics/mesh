package com.gentics.mesh.core.data.dao;

import java.util.Set;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public interface RoleDaoWrapper extends RoleDao, DaoWrapper<HibRole>, DaoTransformable<HibRole, RoleResponse> {

	/**
	 * Create a new role with the given name.
	 * 
	 * @param name
	 *            Name of the new role.
	 * @param creator
	 *            User that is being used to set the reference fields
	 * @return Created role
	 */
	default HibRole create(String name, HibUser creator) {
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
	HibRole create(String name, HibUser creator, String uuid);

	HibRole create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Grant the given permissions on the vertex.
	 *
	 * @param role
	 * @param vertex
	 * @param permissions
	 */
	void grantPermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the vertex.
	 *
	 * @param role
	 * @param vertex
	 * @param permissions
	 */
	void revokePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Return a set of permissions which the role is granting to the given element.
	 *
	 * @param role
	 * @param element
	 * @return Set of permissions of the element
	 */
	Set<InternalPermission> getPermissions(HibRole role, HibBaseElement element);

	/**
	 * Add the given role to this aggregation vertex.
	 * 
	 * @param role
	 *            HibRoleto be added
	 */
	void addRole(HibRole role);

	/**
	 * Remove the given role from this aggregation vertex.
	 * 
	 * @param role
	 *            HibRoleto be removed
	 */
	void removeRole(HibRole role);

	Result<? extends HibGroup> getGroups(HibRole role);

	/**
	 * Return a page of groups to which this role was assigned.
	 *
	 * @param role
	 * @param user
	 * @param params
	 * @return Loaded page
	 */
	Page<? extends HibGroup> getGroups(HibRole role, HibUser user, PagingParameters params);

	/**
	 * Check whether the role grants the given permission on the given element.
	 *
	 * @param role
	 * @param permission
	 * @param element
	 * @return
	 */
	boolean hasPermission(HibRole role, InternalPermission permission, HibBaseElement element);

	HibRole findByUuid(String uuid);

	HibRole findByName(String roleName);

	void delete(HibRole role, BulkActionContext bac);

	HibRole loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

	HibRole loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	boolean update(HibRole role, InternalActionContext ac, EventQueueBatch batch);

	Result<? extends HibRole> findAll();

	TransformablePage<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Role> extraFilter);

	String getETag(HibRole role, InternalActionContext ac);

	String getAPIPath(HibRole role, InternalActionContext ac);

	void applyPermissions(HibBaseElement element, EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke);

}
