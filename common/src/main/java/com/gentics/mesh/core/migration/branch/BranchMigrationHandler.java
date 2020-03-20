package com.gentics.mesh.core.migration.branch;

import com.gentics.mesh.context.BranchMigrationContext;

import io.reactivex.Completable;

public interface BranchMigrationHandler {

	/**
	 * Migrate all nodes from one branch to the other
	 * 
	 * @param context
	 * @return
	 */
	Completable migrateBranch(BranchMigrationContext context);

}
