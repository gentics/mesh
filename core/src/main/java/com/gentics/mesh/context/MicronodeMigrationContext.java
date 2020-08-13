package com.gentics.mesh.context;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;

/**
 * Context of a micronode migration.
 */
public interface MicronodeMigrationContext {

	HibBranch getBranch();

	MigrationStatusHandler getStatus();

	MicroschemaVersion getFromVersion();

	MicroschemaVersion getToVersion();

	/**
	 * Return the cause info of the migration which can be used for dispatched events.
	 * 
	 * @return
	 */
	MicroschemaMigrationCause getCause();

	/**
	 * Validate that all needed information are present in the context.
	 */
	void validate();

}
