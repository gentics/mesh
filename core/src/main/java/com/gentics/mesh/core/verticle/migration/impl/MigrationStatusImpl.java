package com.gentics.mesh.core.verticle.migration.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationStatus;
import com.gentics.mesh.core.verticle.migration.MigrationType;
import com.gentics.mesh.util.DateUtils;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * The migration status class keeps track of the status of a migration and manages also the errors and event handling.
 */
public class MigrationStatusImpl extends AbstractMigrationStatus {

	public MigrationStatusImpl(Message<Object> message, Vertx vertx, MigrationType type) {
		super(message, vertx, type);
	}

	@Override
	public JsonObject createInfoJson() {
		JsonObject info = new JsonObject();
		info.put("type", getType());
		info.put("status", getStatus());
		info.put("startDate", DateUtils.toISO8601(getStartTime()));
		info.put("sourceName", getSourceName());
		info.put("sourceUuid", getSourceUuid());
		String version = getSourceVersion();
		if (version != null) {
			info.put("sourceVersion", getSourceVersion());
		}
		String targetVersion = getTargetVersion();
		if (targetVersion != null) {
			info.put("targetVersion", targetVersion);
		}
		info.put("total", getTotalElements());
		info.put("done", getDoneElements());
		info.put("nodeId", Mesh.mesh().getOptions().getNodeName());
		return info;
	}

	@Override
	public void updateStatus() {
		updateStatus(createInfoJson());
	}

}
