package com.gentics.mesh.core.rest.event.migration;

import com.gentics.mesh.core.rest.branch.BranchReference;

public class BranchMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	private BranchReference fromBranch;

	private BranchReference toBranch;

	public BranchMigrationMeshEventModel() {
	}

	public BranchReference getFromBranch() {
		return fromBranch;
	}

	public void setFromBranch(BranchReference fromBranch) {
		this.fromBranch = fromBranch;
	}

	public BranchReference getToBranch() {
		return toBranch;
	}

	public void setToBranch(BranchReference toBranch) {
		this.toBranch = toBranch;
	}
}
