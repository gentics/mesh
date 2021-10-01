package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
/**
 * DAO for roles
 * 
 * @param <T>
 */
public interface Dao<T> {

	/**
	 * Return the permission info for the given element and role.
	 * 
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(HibCoreElement<? extends RestModel> element, InternalActionContext ac, String roleUuid);

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

	/**
	 * Compare both values in order to determine whether the stored value should be updated.
	 * 
	 * @param restValue
	 *            Rest model value
	 * @param dbValue
	 *            Stored value
	 * @return true if restValue is not null and the restValue is not equal to the stored value. Otherwise false.
	 */
	default <E> boolean shouldUpdate(E restValue, E dbValue) {
		return restValue != null && !restValue.equals(dbValue);
	}
}
