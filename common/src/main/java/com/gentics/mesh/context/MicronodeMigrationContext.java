package com.gentics.mesh.context;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;

/**
 * Context of a micronode migration.
 */
public interface MicronodeMigrationContext {

	Branch getBranch();

	MigrationStatusHandler getStatus();

	MicroschemaContainerVersion getFromVersion();

	MicroschemaContainerVersion getToVersion();

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

	void setBranch(Branch branch);

	void setCause(MicroschemaMigrationCause cause);

	void setToVersion(MicroschemaContainerVersion toVersion);

	void setFromVersion(MicroschemaContainerVersion fromVersion);

	void setStatus(MigrationStatusHandler status);

}
