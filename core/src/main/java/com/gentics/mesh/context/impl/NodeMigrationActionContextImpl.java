package com.gentics.mesh.context.impl;

import java.util.Objects;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;

/**
 * Action context implementation which will be used within the node migration.
 */
public class NodeMigrationActionContextImpl extends AbstractFakeUserInternalActionContext implements NodeMigrationActionContext {

	private Project project;

	private Branch branch;

	private SchemaMigrationCause cause;

	private SchemaContainerVersion fromContainerVersion;

	private SchemaContainerVersion toContainerVersion;

	private MigrationStatusHandler status;

	@Override
	public Branch getBranch() {
		return branch;
	}

	@Override
	public Project getProject() {
		return project;
	}

	/**
	 * Set the project
	 *
	 * @param project
	 */
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Branch getBranch(Project project) {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

	@Override
	public boolean isMigrationContext() {
		return true;
	}

	@Override
	public SchemaMigrationCause getCause() {
		return cause;
	}

	public void setCause(SchemaMigrationCause cause) {
		this.cause = cause;
	}

	@Override
	public SchemaContainerVersion getFromVersion() {
		return fromContainerVersion;
	}

	public void setFromVersion(SchemaContainerVersion fromContainerVersion) {
		this.fromContainerVersion = fromContainerVersion;
	}

	@Override
	public SchemaContainerVersion getToVersion() {
		return toContainerVersion;
	}

	public void setToVersion(SchemaContainerVersion toContainerVersion) {
		this.toContainerVersion = toContainerVersion;
	}

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	@Override
	public MigrationStatusHandler getStatus() {
		return status;
	}

	@Override
	public void validate() {
		Objects.requireNonNull(fromContainerVersion, "The source schema reference is missing in the context.");
		Objects.requireNonNull(toContainerVersion, "The target schema reference is missing in the context.");
	}

	@Override
	public boolean isPurgeAllowed() {
		// The purge operation is not allowed during schema migrations. Instead the purge will be executed after containers have been migrated.
		return false;
	}

}
