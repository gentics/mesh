package com.syncleus.ferma.ext.orientdb3;

import com.gentics.mesh.cli.ODBBootstrapInitializer;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PermissionRoots;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @see PermissionRoots
 */
@Singleton
public class PermissionRootsImpl implements PermissionRoots {
	private final ODBBootstrapInitializer boot;

	@Inject
	public PermissionRootsImpl(ODBBootstrapInitializer boot) {
		this.boot = boot;
	}

	@Override
	public HibBaseElement project() {
		return boot.projectRoot();
	}

	@Override
	public HibBaseElement user() {
		return boot.userRoot();
	}

	@Override
	public HibBaseElement group() {
		return boot.groupRoot();
	}

	@Override
	public HibBaseElement role() {
		return boot.roleRoot();
	}

	@Override
	public HibBaseElement microschema() {
		return boot.microschemaContainerRoot();
	}

	@Override
	public HibBaseElement schema() {
		return boot.schemaContainerRoot();
	}
}
