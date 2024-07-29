package com.gentics.mesh.core.migration;

import java.util.List;

import com.gentics.mesh.context.BranchMigrationContext;

import com.gentics.mesh.core.data.node.HibNode;
import io.reactivex.Completable;

public interface BranchMigration {

	/**
	 * Migrate all nodes from one branch to the other
	 * 
	 * @param context
	 * @return
	 */
	Completable migrateBranch(BranchMigrationContext context);

	/**
	 * Called before batch migrating to prepare the nodes for the migration.
	 * The returned nodes are the one that will be used for the migration
	 * @param nodes
	 * @return
	 */
	default List<? extends HibNode> beforeBatchMigration(List<? extends HibNode> nodes) {
		return nodes;
	}

}
