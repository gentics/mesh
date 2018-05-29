package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.UNKNOWN;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.branch.ReleaseVersionEdge;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;

/**
 * Abstract implementation for {@link BranchMicroschemaEdgeImpl} and {@link BranchSchemaEdgeImpl}.
 */
public abstract class AbstractVersionEdge extends MeshEdgeImpl implements ReleaseVersionEdge {

	@Override
	public void setMigrationStatus(MigrationStatus status) {
		setProperty(MIGRATION_STATUS_PROPERTY_KEY, status.name());
	}

	@Override
	public MigrationStatus getMigrationStatus() {
		String status = getProperty(MIGRATION_STATUS_PROPERTY_KEY);
		if (status == null) {
			return UNKNOWN;
		}
		return MigrationStatus.valueOf(status);
	}

	@Override
	public Branch getRelease() {
		return outV().nextOrDefaultExplicit(BranchImpl.class, null);
	}

	@Override
	public String getJobUuid() {
		return getProperty(JOB_UUID_PROPERTY_KEY);
	}

	@Override
	public void setJobUuid(String uuid) {
		setProperty(JOB_UUID_PROPERTY_KEY, uuid);
	}

	@Override
	public boolean isActive() {
		return getProperty(ACTIVE_PROPERTY_KEY);
	}

	@Override
	public void setActive(boolean active) {
		setProperty(ACTIVE_PROPERTY_KEY, active);
	}

}
