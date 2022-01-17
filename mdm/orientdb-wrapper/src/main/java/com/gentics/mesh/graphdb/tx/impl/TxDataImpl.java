package com.gentics.mesh.graphdb.tx.impl;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.CommonTxData;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

import io.vertx.core.Vertx;

/**
 * @see TxData
 */
public class TxDataImpl implements CommonTxData {

	private final OrientDBBootstrapInitializer boot;
	private final OrientDBMeshOptions options;
	private final PermissionRoots permissionRoots;

	@Inject
	public TxDataImpl(OrientDBMeshOptions options, OrientDBBootstrapInitializer boot,
			PermissionRoots permissionRoots) {
		this.options = options;
		this.boot = boot;
		this.permissionRoots = permissionRoots;
	}

	@Override
	public OrientDBMeshComponent mesh() {
		return boot.mesh().internal();
	}

	@Override
	public OrientDBMeshOptions options() {
		return options;
	}

	@Override
	public HibMeshVersion meshVersion() {
		return boot.meshRoot();
	}

	@Override
	public PermissionRoots permissionRoots() {
		return permissionRoots;
	}

	@Override
	public Vertx vertx() {
		return boot.vertx();
	}

	@Override
	public ServerSchemaStorage serverSchemaStorage() {
		return mesh().serverSchemaStorage();
	}

	@Override
	public BinaryStorage binaryStorage() {
		return mesh().binaryStorage();
	}
}
