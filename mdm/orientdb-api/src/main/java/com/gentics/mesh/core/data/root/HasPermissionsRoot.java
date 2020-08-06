package com.gentics.mesh.core.data.root;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface HasPermissionsRoot {
	/**
	 * Get the permissions of a role for this element.
	 *
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid);

	/**
	 * Return a traversal result for all roles which grant the permission to the element.
	 *
	 * @param perm
	 * @return
	 */
	TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm);
}
