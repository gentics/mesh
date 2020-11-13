package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;

public interface PermissionProperties {

	PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid);

	Result<? extends HibRole> getRolesWithPerm(HibBaseElement element, InternalPermission perm);

}
