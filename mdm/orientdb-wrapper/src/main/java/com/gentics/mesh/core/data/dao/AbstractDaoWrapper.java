package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericRestResponse;

import dagger.Lazy;

/**
 * Abstract implementation for DAO's.
 * 
 * @param <T>
 */
public abstract class AbstractDaoWrapper<T extends HibBaseElement> implements Dao<T> {

	protected final Lazy<OrientDBBootstrapInitializer> boot;

	public AbstractDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot) {
		this.boot = boot;
	}

	@Override
	public void setRolePermissions(T element, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(Tx.get().roleDao().getRolePermissions(element, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}
}
