package com.gentics.mesh.context.impl;

import java.util.Objects;

import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;

public class MicronodeMigrationContextImpl implements MicronodeMigrationContext {

	private MigrationStatusHandler status;

	private HibBranch branch;

	private MicroschemaVersion fromVersion;

	private MicroschemaVersion toVersion;

	private MicroschemaMigrationCause cause;

	@Override
	public MigrationStatusHandler getStatus() {
		return status;
	}

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	@Override
	public HibBranch getBranch() {
		return branch;
	}

	public void setBranch(HibBranch branch) {
		this.branch = branch;
	}

	@Override
	public MicroschemaVersion getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(MicroschemaVersion fromVersion) {
		this.fromVersion = fromVersion;
	}

	@Override
	public MicroschemaVersion getToVersion() {
		return toVersion;
	}

	public void setToVersion(MicroschemaVersion toVersion) {
		this.toVersion = toVersion;
	}

	@Override
	public MicroschemaMigrationCause getCause() {
		return cause;
	}

	public void setCause(MicroschemaMigrationCause cause) {
		this.cause = cause;
	}

	@Override
	public void validate() {
		Objects.requireNonNull(fromVersion, "The from microschema version reference is missing in the context.");
		Objects.requireNonNull(toVersion, "The target microschema version reference is missing in the context.");
		Objects.requireNonNull(branch, "The branch reference is missing in the context.");
	}

}
