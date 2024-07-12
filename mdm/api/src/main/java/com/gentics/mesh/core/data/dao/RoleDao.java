package com.gentics.mesh.core.data.dao;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

/**
 * DAO for {@link Role}.
 */
public interface RoleDao extends DaoGlobal<Role>, DaoTransformable<Role, RoleResponse> {

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

	/**
	 * Create a new role
	 *
	 * @param ac
	 * @param batch
	 * @param uuid
	 *            Uuid of the role
	 * @return
	 */
	Role create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Grant the given permissions on the given role.
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 * @return
	 */
	boolean grantPermissions(Role role, BaseElement element, InternalPermission... permissions);

	/**
	 * Grant the given permissions on the element to the set of roles
	 *
	 * @param roles set of roles
	 * @param element element to grant permission on
	 * @param exclusive true to revoke the given permissions on all other roles
	 * @param permissions permissions to grant
	 * @return true, iff permissions were effectively changed
	 */
	boolean grantPermissions(Set<Role> roles, BaseElement element, boolean exclusive, InternalPermission... permissions);

	/**
	 * Grant the given permissions on the element to the set of roles (identified by their uuids)
	 * 
	 * @param roleUuids set of role uuids
	 * @param element element to grant permission on
	 * @param exclusive true to revoke the given permissions on all other roles
	 * @param permissions permissions to grant
	 * @return true, iff permissions where effectively changed
	 */
	boolean grantPermissionsWithUuids(Set<String> roleUuids, BaseElement element, boolean exclusive, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the given role.
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 * @return
	 */
	boolean revokePermissions(Role role, BaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the element from the given roles.
	 *
	 * @param roles set of roles
	 * @param element element to revoke permissions from
	 * @param permissions permissions to revoke
	 * @return true, iff permissions were effectively changed
	 */
	boolean revokePermissions(Set<Role> roles, BaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the element from the given roles (identified by their uuids)
	 *
	 * @param roleUuids set of role uuids
	 * @param element element to revoke permissions from
	 * @param permissions permissions to revoke
	 * @return true, iff permissions were effectively changed
	 */
	boolean revokePermissionsWithUuids(Set<String> roleUuids, BaseElement element, InternalPermission... permissions);

	/**
	 * Return a set of permissions which the role is granting to the given element.
	 *
	 * @param role
	 * @param element
	 * @return Set of permissions of the element
	 */
	Set<InternalPermission> getPermissions(Role role, BaseElement element);

	/**
	 * Return the sets of granted permissions to the given set of roles on the given element
	 *
	 * @param roles set of roles
	 * @param element element
	 * @return map of permission sets per role
	 */
	Map<Role, Set<InternalPermission>> getPermissions(Set<Role> roles, BaseElement element);

	/**
	 * Add the given role to this role.
	 * 
	 * @param role
	 *            HibRoleto be added
	 */
	void addRole(Role role);

	/**
	 * Remove the given role from this role.
	 * 
	 * @param role
	 *            HibRoleto be removed
	 */
	void removeRole(Role role);

	/**
	 * Return all groups for the given role.
	 * 
	 * @param role
	 * @return
	 */
	Result<? extends Group> getGroups(Role role);

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
	boolean hasPermission(Role role, InternalPermission permission, BaseElement element);

	/**
	 * Apply the permissions for the given element.
	 *
	 * @param authUser
	 * @param element
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	void applyPermissions(MeshAuthUser authUser, BaseElement element, EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
						  Set<InternalPermission> permissionsToRevoke);

	/**
	 * Return set of role uuids for the given permission that were granted on the element.
	 *
	 * @param element
	 * @param permission
	 * @return
	 */
	Set<String> getRoleUuidsForPerm(BaseElement element, InternalPermission permission);

	/**
	 * Return the roles which grant the given permission on the element.
	 * 
	 * @param element
	 * @param perm
	 * @return
	 */
	Result<? extends Role> getRolesWithPerm(BaseElement element, InternalPermission perm);

	/**
	 * Return the permission info for the given element and role.
	 * 
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(BaseElement element, InternalActionContext ac, String roleUuid);

	/**
	 * Set the role permission for the given element.
	 * 
	 * @param element
	 * @param ac
	 * @param model
	 */
	void setRolePermissions(BaseElement element, InternalActionContext ac, GenericRestResponse model);

	/**
	 * Find all existing roles, the current user is allowed to see. Return as list of triples (uuid, name, internal ID)
	 * @param ac action context
	 * @return list of triples (uuid, name, internal ID)
	 */
	default Set<Triple<String, String, Object>> findAll(InternalActionContext ac) {
		return findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream()
				.map(role -> Triple.of(role.getUuid(), role.getName(), role.getId())).collect(Collectors.toSet());
	}
}
