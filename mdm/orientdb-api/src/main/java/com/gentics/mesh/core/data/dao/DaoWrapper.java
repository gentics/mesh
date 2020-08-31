package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;

public interface DaoWrapper<T> extends DaoGlobal<T> {

	PermissionInfo getRolePermissions(HibCoreElement element, InternalActionContext ac, String roleUuid);

	Result<? extends HibRole> getRolesWithPerm(T element, InternalPermission perm);

	void setRolePermissions(T element, InternalActionContext ac, GenericRestResponse model);

}
