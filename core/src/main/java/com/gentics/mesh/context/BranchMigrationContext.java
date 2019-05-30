package com.gentics.mesh.context;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;

/**
 * Context of the branch migration which contains information about the migration.
 */
public interface BranchMigrationContext {

	/**
	 * Return the old branch from which to migrate.
	 * 
	 * @return
	 */
	Branch getOldBranch();

	/**
	 * Return the new branch to migrate to.
	 * 
	 * @return
	 */
	Branch getNewBranch();

	/**
	 * Return the migration cause info.
	 * 
	 * @return
	 */
	BranchMigrationCause getCause();

	/**
	 * Return the migration status handler.
	 * 
	 * @return
	 */
	MigrationStatusHandler getStatus();

	/**
	 * Validate that all needed information are present in the context.
	 */
	void validate();

}
