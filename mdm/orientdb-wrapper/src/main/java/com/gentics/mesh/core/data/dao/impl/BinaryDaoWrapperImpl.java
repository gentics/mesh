package com.gentics.mesh.core.data.dao.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;

import dagger.Lazy;

@Singleton
public class BinaryDaoWrapperImpl extends AbstractDaoWrapper implements BinaryDaoWrapper {

	@Inject
	public BinaryDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

}
