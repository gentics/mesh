package com.gentics.mesh.context;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;

public interface NodeMigrationActionContext extends InternalActionContext {

	/**
	 * Return referenced project.
	 * 
	 * @return current project
	 */
	HibProject getProject();

	/**
	 * Return referenced branch.
	 * 
	 * @return branch
	 */
	HibBranch getBranch();

	/**
	 * Return the from schema version.
	 * 
	 * @return version
	 */
	HibSchemaVersion getFromVersion();

	/**
	 * Return the to schema version.
	 * 
	 * @return version
	 */
	HibSchemaVersion getToVersion();

	/**
	 * Return migration cause.
	 * 
	 * @return cause
	 */
	SchemaMigrationCause getCause();

	/**
	 * Return the status handler.
	 * 
	 * @return status
	 */
	MigrationStatusHandler getStatus();

	/**
	 * Validate that all needed information is present in the context.
	 */
	void validate();
}
