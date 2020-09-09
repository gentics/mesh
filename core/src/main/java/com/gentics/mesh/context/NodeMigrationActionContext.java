package com.gentics.mesh.context;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;

public interface NodeMigrationActionContext extends InternalActionContext {

	/**
	 * Return referenced project.
	 * 
	 * @return current project
	 */
	Project getProject();

	/**
	 * Return referenced branch.
	 * 
	 * @return branch
	 */
	Branch getBranch();

	/**
	 * Return the from schema version.
	 * 
	 * @return version
	 */
	SchemaContainerVersion getFromVersion();

	/**
	 * Return the to schema version.
	 * 
	 * @return version
	 */
	SchemaContainerVersion getToVersion();

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
