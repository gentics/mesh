package com.gentics.mesh.context.impl;

import java.util.Objects;

import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;

/**
 * @see BranchMigrationContext
 */
public class BranchMigrationContextImpl implements BranchMigrationContext {

	private BranchMigrationCause cause;

	private HibBranch newBranch;

	private MigrationStatusHandler status;

	private HibBranch oldBranch;

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
	public HibBranch getNewBranch() {
		return newBranch;
	}

	public void setNewBranch(HibBranch newBranch) {
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
	public HibBranch getOldBranch() {
		return oldBranch;
	}

	public void setOldBranch(HibBranch oldBranch) {
		this.oldBranch = oldBranch;
	}

	@Override
	public void validate() {
		Objects.requireNonNull(oldBranch, "The old branch reference is missing in the context.");
		Objects.requireNonNull(newBranch, "The new branch reference is missing in the context.");
	}

}
