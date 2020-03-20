package com.gentics.mesh.context;

import java.util.Set;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;

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

	void setStatus(MigrationStatusHandler status);

	void setFromVersion(SchemaContainerVersion fromContainerVersion);

	void setCause(SchemaMigrationCause cause);

	/**
	 * Set the project
	 *
	 * @param project
	 */
	void setProject(Project project);

	void setBranch(Branch branch);

	void setToVersion(SchemaContainerVersion toContainerVersion);

	/**
	 * Get the set of encountered conflicts.
	 *
	 * @return
	 */
	Set<ConflictWarning> getConflicts();

	/**
	 * Add the encountered conflict info to the context.
	 *
	 * @param info
	 */
	void addConflictInfo(ConflictWarning info);
}
