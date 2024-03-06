package com.syncleus.ferma.ext.orientdb3;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PermissionRoots;

/**
 * @see PermissionRoots
 */
@Singleton
public class PermissionRootsImpl implements PermissionRoots {
	private final OrientDBBootstrapInitializer boot;

	@Inject
	public PermissionRootsImpl(OrientDBBootstrapInitializer boot) {
		this.boot = boot;
	}

	@Override
	public HibBaseElement project() {
		return boot.meshRoot().getProjectRoot();
	}

	@Override
	public HibBaseElement user() {
		return boot.meshRoot().getUserRoot();
	}

	@Override
	public HibBaseElement group() {
		return boot.meshRoot().getGroupRoot();
	}

	@Override
	public HibBaseElement role() {
		return boot.meshRoot().getRoleRoot();
	}

	@Override
	public HibBaseElement microschema() {
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public HibBaseElement schema() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

	@Override
	public HibBaseElement mesh() {
		return boot.meshRoot();
	}
}
