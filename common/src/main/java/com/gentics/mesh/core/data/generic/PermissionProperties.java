package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface PermissionProperties {

	TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm);

	PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid);

}
