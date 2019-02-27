package com.gentics.mesh.context;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;

public interface NodeMigrationActionContext {

	/**
	 * Return referenced project.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Return referenced branch.
	 * 
	 * @return
	 */
	Branch getBranch();

	/**
	 * Return the from schema version.
	 * 
	 * @return
	 */
	SchemaContainerVersion getFromVersion();

	/**
	 * Return the to schema version.
	 * 
	 * @return
	 */
	SchemaContainerVersion getToVersion();

	/**
	 * Return migration cause.
	 * 
	 * @return
	 */
	SchemaMigrationCause getCause();

	/**
	 * Return the status handler.
	 * 
	 * @return
	 */
	MigrationStatusHandler getStatus();

	/**
	 * Validate that all needed information is present in the context.
	 */
	void validate();
}
