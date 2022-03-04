package com.gentics.mesh.core.data.dao;

import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link HibRole}.
 */
public interface RoleDao extends DaoGlobal<HibRole>, DaoTransformable<HibRole, RoleResponse> {

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
	 * Grant the given permissions on the given role.
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 * @return
	 */
	boolean grantPermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the given role.
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 * @return
	 */
	boolean revokePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Return a set of permissions which the role is granting to the given element.
	 *
	 * @param role
	 * @param element
	 * @return Set of permissions of the element
	 */
	Set<InternalPermission> getPermissions(HibRole role, HibBaseElement element);

	/**
	 * Add the given role to this role.
	 * 
	 * @param role
	 *            HibRoleto be added
	 */
	void addRole(HibRole role);

	/**
	 * Remove the given role from this role.
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

	/**
	 * Return the roles which grant the given permission on the element.
	 * 
	 * @param element
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(HibBaseElement element, InternalPermission perm);

	/**
	 * Return the permission info for the given element and role.
	 * 
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid);

	/**
	 * Set the role permission for the given element.
	 * 
	 * @param element
	 * @param ac
	 * @param model
	 */
	void setRolePermissions(HibBaseElement element, InternalActionContext ac, GenericRestResponse model);
}
