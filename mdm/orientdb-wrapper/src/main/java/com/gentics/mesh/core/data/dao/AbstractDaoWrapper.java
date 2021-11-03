package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;

import dagger.Lazy;

/**
 * Abstract implementation for DAO's.
 * 
 * @param <T>
 */
public abstract class AbstractDaoWrapper<T extends HibBaseElement> implements Dao<T> {

	protected final Lazy<OrientDBBootstrapInitializer> boot;

	protected final Lazy<PermissionPropertiesImpl> permissions;

	public AbstractDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		this.boot = boot;
		this.permissions = permissions;
	}

	@Override
	public PermissionInfo getRolePermissions(HibCoreElement<? extends RestModel> element, InternalActionContext ac, String roleUuid) {
		return permissions.get().getRolePermissions(element, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(T element, InternalPermission perm) {
		return permissions.get().getRolesWithPerm(element, perm);
	}

	@Override
	public void setRolePermissions(T element, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(permissions.get().getRolePermissions(element, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}
}
