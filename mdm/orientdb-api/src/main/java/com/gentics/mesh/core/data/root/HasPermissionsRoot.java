package com.gentics.mesh.core.data.root;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;

public interface HasPermissionsRoot {
	/**
	 * Get the permissions of a role for this element.
	 *
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(HibElement element, InternalActionContext ac, String roleUuid);

	/**
	 * Return a traversal result for all roles which grant the permission to the element.
	 *
	 * @param element
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(HibElement element, InternalPermission perm);
}
