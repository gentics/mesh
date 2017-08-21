package com.gentics.mesh.core.rest.admin;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Model for the migration status information.
 */
public class MigrationInfo implements RestModel {

	private String startDate;
	private String sourceName;
	private String sourceUuid;
	private MigrationType type;
	private String nodeName;
	private int done;
	private int total;
	private String targetVersion;
	private String sourceVersion;
	private String status;

	public String getStartDate() {
		return startDate;
	}

	public MigrationInfo setStartDate(String startDate) {
		this.startDate = startDate;
		return this;
	}

	public MigrationType getType() {
		return type;
	}

	public MigrationInfo setType(MigrationType type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public MigrationInfo setStatus(String status) {
		this.status = status;
		return this;
	}

	public String getSourceUuid() {
		return sourceUuid;
	}

	public MigrationInfo setSourceUuid(String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	public String getSourceName() {
		return sourceName;
	}

	public MigrationInfo setSourceName(String sourceName) {
		this.sourceName = sourceName;
		return this;
	}

	public String getSourceVersion() {
		return sourceVersion;
	}

	public MigrationInfo setSourceVersion(String sourceVersion) {
		this.sourceVersion = sourceVersion;
		return this;
	}

	public String getTargetVersion() {
		return targetVersion;
	}

	public MigrationInfo setTargetVersion(String targetVersion) {
		this.targetVersion = targetVersion;
		return this;
	}

	public int getTotal() {
		return total;
	}

	public MigrationInfo setTotal(int total) {
		this.total = total;
		return this;
	}

	public int getDone() {
		return done;
	}

	public MigrationInfo setDone(int done) {
		this.done = done;
		return this;
	}

	public String getNodeName() {
		return nodeName;
	}

	public MigrationInfo setNodeId(String nodeName) {
		this.nodeName = nodeName;
		return this;
	}

}
