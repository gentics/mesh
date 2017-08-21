package com.gentics.mesh.core.verticle.migration.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.admin.MigrationInfo;
import com.gentics.mesh.core.rest.admin.MigrationType;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.util.DateUtils;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
 * The migration status class keeps track of the status of a migration and manages also the errors and event handling.
 */
public class MigrationStatusHandlerImpl extends AbstractMigrationStatusHandler {

	public MigrationStatusHandlerImpl(Message<Object> message, Vertx vertx, MigrationType type) {
		super(message, vertx, type);
	}

	@Override
	public MigrationInfo createInfo() {
		MigrationInfo info = new MigrationInfo();
		info.setType(getType());
		info.setStatus(getStatus());
		info.setStartDate(DateUtils.toISO8601(getStartTime()));
		info.setSourceName(getSourceName());
		info.setSourceUuid(getSourceUuid());
		String version = getSourceVersion();
		if (version != null) {
			info.setSourceVersion(getSourceVersion());
		}
		String targetVersion = getTargetVersion();
		if (targetVersion != null) {
			info.setTargetVersion(targetVersion);
		}
		info.setTotal(getTotalElements());
		info.setDone(getDoneElements());
		info.setNodeId(Mesh.mesh().getOptions().getNodeName());
		return info;
	}

	@Override
	public MigrationStatusHandler updateStatus() {
		updateStatus(createInfo());
		return this;
	}

}
