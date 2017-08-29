package com.gentics.mesh.core.rest.admin.migration;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.IDLE;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.impl.ClusterSerializable;

public class MigrationStatusResponse implements RestModel, ClusterSerializable {

	private List<MigrationInfo> migrations = new ArrayList<MigrationInfo>();

	/**
	 * Return the status of the latest migration.
	 * 
	 * @return
	 */
	public MigrationStatus getStatus() {
		MigrationStatus latestStatus = getMigrations().stream().sorted().reduce((first, second) -> second).map(e -> e.getStatus()).orElse(IDLE);
		// Some stati describe the end of a migration. This means other migrations could be executed. Thus we return idle in those cases.
		switch (latestStatus) {
		case COMPLETED:
		case FAILED:
			return MigrationStatus.IDLE;
		default:
			return latestStatus;
		}
	}

	/**
	 * Return the list of recently migrations.
	 * 
	 * @return
	 */
	public List<MigrationInfo> getMigrations() {
		return migrations;
	}

	/**
	 * Set the list of migrations.
	 * 
	 * @param migrations
	 */
	public MigrationStatusResponse setMigrations(List<MigrationInfo> migrations) {
		this.migrations = migrations;
		return this;
	}

	public int readFromBuffer(int pos, Buffer buffer) {
		int length = buffer.getInt(pos);
		int start = pos + 4;
		String encoded = buffer.getString(start, start + length);
		MigrationStatusResponse fromBuffer = JsonUtil.readValue(encoded, getClass());
		// this.setStatus(fromBuffer.getStatus());
		this.setMigrations(fromBuffer.getMigrations());
		return pos + length + 4;
	}

	public void writeToBuffer(Buffer buffer) {
		String encoded = toJson();
		byte[] bytes = encoded.getBytes(StandardCharsets.UTF_8);
		buffer.appendInt(bytes.length);
		buffer.appendBytes(bytes);
	}

}
