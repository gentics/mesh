package com.gentics.mesh.core.data.dao;

import java.util.Set;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public interface RoleDao extends Dao<HibRole>, DaoTransformable<HibRole, RoleResponse> {

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

	/**
	 * Create a new role
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 *            Uuid of the role
	 * @return
	 */
	HibRole create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Grant the given permissions on the vertex.
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 */
	void grantPermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the vertex.
	 *
	 * @param role
	 * @param element
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

	/**
	 * Return all groups for the given role.
	 * 
	 * @param role
	 * @return
	 */
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

	/**
	 * Find the role by uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	HibRole findByUuid(String uuid);

	/**
	 * Find the role by name.
	 * 
	 * @param roleName
	 * @return
	 */
	HibRole findByName(String roleName);

	/**
	 * Delete the role.
	 * 
	 * @param role
	 * @param bac
	 */
	void delete(HibRole role, BulkActionContext bac);

	/**
	 * Load the role by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	HibRole loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

	/**
	 * Load the role by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	HibRole loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Update the role
	 * 
	 * @param role
	 *            Role to update
	 * @param ac
	 *            Context which holds the payload
	 * @param batch
	 * @return true, when the role has been updated. Otherwise false.
	 */
	boolean update(HibRole role, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Return all roles.
	 * 
	 * @return
	 */
	Result<? extends HibRole> findAll();

	/**
	 * Load a page of roles.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of roles.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibRole> extraFilter);

	/**
	 * Return the etag for the given role.
	 * 
	 * @param role
	 * @param ac
	 * @return
	 */
	String getETag(HibRole role, InternalActionContext ac);

	/**
	 * Return the api path for the role.
	 * 
	 * @param role
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibRole role, InternalActionContext ac);

	/**
	 * Apply the permissions for the given element.
	 * 
	 * @param element
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	void applyPermissions(HibBaseElement element, EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke);

	/**
	 * Return set of role uuids for the given permission that were granted on the element.
	 *
	 * @param element
	 * @param permission
	 * @return
	 */
	Set<String> getRoleUuidsForPerm(HibBaseElement element, InternalPermission permission);

}
