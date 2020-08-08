package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.rest.common.GenericRestResponse;

import dagger.Lazy;

public abstract class AbstractDaoWrapper {

	protected final Lazy<BootstrapInitializer> boot;

	protected final Lazy<PermissionProperties> permissions;

	public AbstractDaoWrapper(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		this.boot = boot;
		this.permissions = permissions;
	}

	/**
	 * Compare both values in order to determine whether the graph value should be updated.
	 * 
	 * @param restValue
	 *            Rest model string value
	 * @param graphValue
	 *            Graph string value
	 * @return true if restValue is not null and the restValue is not equal to the graph value. Otherwise false.
	 */
	protected <T> boolean shouldUpdate(T restValue, T graphValue) {
		return restValue != null && !restValue.equals(graphValue);
	}

	protected void setRolePermissions(MeshVertex vertex, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(permissions.get().getRolePermissions(vertex, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

}
