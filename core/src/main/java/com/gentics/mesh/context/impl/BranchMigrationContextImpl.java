package com.gentics.mesh.context.impl;

import java.util.Objects;

import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;

public class BranchMigrationContextImpl implements BranchMigrationContext {

	private BranchMigrationCause cause;

	private Branch newBranch;

	private MigrationStatusHandler status;

	private Branch oldBranch;

	public BranchMigrationContextImpl() {
	}

	@Override
	public BranchMigrationCause getCause() {
		return cause;
	}

	public void setCause(BranchMigrationCause cause) {
		this.cause = cause;
	}

	@Override
	public Branch getNewBranch() {
		return newBranch;
	}

	public void setNewBranch(Branch newBranch) {
		this.newBranch = newBranch;
	}

	@Override
	public MigrationStatusHandler getStatus() {
		return status;
	}

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	@Override
	public Branch getOldBranch() {
		return oldBranch;
	}

	public void setOldBranch(Branch oldBranch) {
		this.oldBranch = oldBranch;
	}

	@Override
	public void validate() {
		Objects.requireNonNull(oldBranch, "The old branch reference is missing in the context.");
		Objects.requireNonNull(newBranch, "The new branch reference is missing in the context.");
	}

}
