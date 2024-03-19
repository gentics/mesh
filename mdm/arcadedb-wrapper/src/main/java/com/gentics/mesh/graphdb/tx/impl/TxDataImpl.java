package com.gentics.mesh.graphdb.tx.impl;

import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.cli.GraphDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.db.CommonTxData;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.dagger.ArcadeDBMeshComponent;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.event.EventQueueBatch;

import io.vertx.core.Vertx;

/**
 * @see TxData
 */
public class TxDataImpl implements CommonTxData {

	private final GraphDBBootstrapInitializer boot;
	private final GraphDBMeshOptions options;
	private final PermissionRoots permissionRoots;
	private Optional<EventQueueBatch> qBatch;

	@Inject
	public TxDataImpl(GraphDBMeshOptions options, GraphDBBootstrapInitializer boot,
			PermissionRoots permissionRoots) {
		this.options = options;
		this.boot = boot;
		this.permissionRoots = permissionRoots;
		this.qBatch = Optional.empty();
	}

	@Override
	public ArcadeDBMeshComponent mesh() {
		return boot.mesh().internal();
	}

	@Override
	public GraphDBMeshOptions options() {
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
	public boolean isVertxReady() {
		return boot.isVertxReady();
	}

	@Override
	public ServerSchemaStorage serverSchemaStorage() {
		return mesh().serverSchemaStorage();
	}

	@Override
	public BinaryStorage binaryStorage() {
		return mesh().binaryStorage();
	}

	@Override
	public S3BinaryStorage s3BinaryStorage() {
		return mesh().s3binaryStorage();
	}

	@Override
	public void setEventQueueBatch(EventQueueBatch batch) {
		this.qBatch = Optional.ofNullable(batch);
	}

	@Override
	public Optional<EventQueueBatch> maybeGetEventQueueBatch() {
		return qBatch;
	}
}
