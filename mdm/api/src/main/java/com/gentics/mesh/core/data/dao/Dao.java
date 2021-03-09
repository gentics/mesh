package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;

public interface Dao<T> extends DaoGlobal<T> {

	/**
	 * Return the permission info for the given element and role.
	 * 
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(HibCoreElement element, InternalActionContext ac, String roleUuid);

	/**
	 * Return the roles which grant the given permission on the element.
	 * 
	 * @param element
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(T element, InternalPermission perm);

	/**
	 * Set the role permissionf for the given element.
	 * 
	 * @param element
	 * @param ac
	 * @param model
	 */
	void setRolePermissions(T element, InternalActionContext ac, GenericRestResponse model);

}
