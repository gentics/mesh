package com.gentics.mesh.core.data.dao.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.impl.BinaryWrapper;

import dagger.Lazy;

@Singleton
public class BinaryDaoWrapperImpl implements BinaryDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public BinaryDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

}
