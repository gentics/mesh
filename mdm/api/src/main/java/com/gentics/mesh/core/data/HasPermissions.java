package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;

/**
 * Generic extension for domain models which provide means to load role permissions from them.
 */
public interface HasPermissions {

	/**
	 * Get the permissions of a role for this element.
	 *
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid);

	/**
	 * Return a traversal result for all roles which grant the permission to the element.
	 *
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(InternalPermission perm);
}
