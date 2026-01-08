package com.gentics.mesh.core.data.dao;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

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
	 * @return true, if permissions were effectively changed
	 */
	boolean grantPermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Grant the given permissions on the element to the set of roles
	 *
	 * @param roles set of roles
	 * @param element element to grant permission on
	 * @param exclusive true to revoke the given permissions on all other roles
	 * @param permissions permissions to grant
	 * @return true, if permissions were effectively changed
	 */
	boolean grantPermissions(Set<HibRole> roles, HibBaseElement element, boolean exclusive, InternalPermission... permissions);

	/**
	 * Grant the given permissions on the element to the set of roles (identified by their uuids)
	 * 
	 * @param roleUuids set of role uuids
	 * @param element element to grant permission on
	 * @param exclusive true to revoke the given permissions on all other roles
	 * @param permissions permissions to grant
	 * @return true, if permissions where effectively changed
	 */
	boolean grantPermissionsWithUuids(Set<String> roleUuids, HibBaseElement element, boolean exclusive, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the given role.
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 * @return true, if permissions were effectively changed
	 */
	boolean revokePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the element from the given roles.
	 *
	 * @param roles set of roles
	 * @param element element to revoke permissions from
	 * @param permissions permissions to revoke
	 * @return true, iff permissions were effectively changed
	 */
	boolean revokePermissions(Set<HibRole> roles, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Revoke the given permissions on the element from the given roles (identified by their uuids)
	 *
	 * @param roleUuids set of role uuids
	 * @param element element to revoke permissions from
	 * @param permissions permissions to revoke
	 * @return true, if permissions were effectively changed
	 */
	boolean revokePermissionsWithUuids(Set<String> roleUuids, HibBaseElement element, InternalPermission... permissions);

	/**
	 * Return a set of permissions which the role is granting to the given element.
	 *
	 * @param role
	 * @param element
	 * @return Set of permissions of the element
	 */
	Set<InternalPermission> getPermissions(HibRole role, HibBaseElement element);

	/**
	 * Return the sets of granted permissions to the given set of roles on the given element
	 *
	 * @param roles set of roles
	 * @param element element
	 * @return map of permission sets per role
	 */
	Map<HibRole, Set<InternalPermission>> getPermissions(Set<HibRole> roles, HibBaseElement element);

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
	 * Return the groups for all given roles
	 * @param roles collection of roles
	 * @return map of role to the collections of groups
	 */
	Map<HibRole, Collection<? extends HibGroup>> getGroups(Collection<HibRole> roles);

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
	 * @param authUser
	 * @param element
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	void applyPermissions(MeshAuthUser authUser, HibBaseElement element, EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
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
	 * Return set of role uuids for the given permission that were granted on the given collection of elements.
	 *
	 * @param elements
	 * @param permission
	 * @return
	 */
	Map<HibBaseElement, Set<String>> getRoleUuidsForPerm(Collection<? extends HibBaseElement> elements, InternalPermission permission);

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
