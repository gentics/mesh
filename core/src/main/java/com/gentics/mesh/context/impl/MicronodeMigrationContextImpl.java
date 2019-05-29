package com.gentics.mesh.context.impl;

import java.util.Objects;

import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;

public class MicronodeMigrationContextImpl implements MicronodeMigrationContext {

	private MigrationStatusHandler status;

	private Branch branch;

	private MicroschemaContainerVersion fromVersion;

	private MicroschemaContainerVersion toVersion;

	private MicroschemaMigrationCause cause;

	@Override
	public MigrationStatusHandler getStatus() {
		return status;
	}

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	@Override
	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

	@Override
	public MicroschemaContainerVersion getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(MicroschemaContainerVersion fromVersion) {
		this.fromVersion = fromVersion;
	}

	@Override
	public MicroschemaContainerVersion getToVersion() {
		return toVersion;
	}

	public void setToVersion(MicroschemaContainerVersion toVersion) {
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
