package com.gentics.mesh.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshFactory;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;

/**
 * Factory which will create and maintain the state of a single mesh instance.
 */
public class MeshFactoryImpl implements MeshFactory {

	@Override
	public Mesh create() {
		return new MeshImpl(OptionsLoader.createOrloadOptions());
	}

	@Override
	public Mesh create(MeshOptions options, Vertx vertx) {
		return new MeshImpl(options, vertx);
	}

}
