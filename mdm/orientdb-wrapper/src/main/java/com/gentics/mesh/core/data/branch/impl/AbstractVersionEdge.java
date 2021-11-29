package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.rest.job.JobStatus;

/**
 * Abstract implementation for {@link BranchMicroschemaEdgeImpl} and {@link BranchSchemaEdgeImpl}.
 */
public abstract class AbstractVersionEdge extends MeshEdgeImpl implements BranchVersionEdge {

	@Override
	public void setMigrationStatus(JobStatus status) {
		setProperty(MIGRATION_STATUS_PROPERTY_KEY, status.name());
	}

	@Override
	public JobStatus getMigrationStatus() {
		String status = getProperty(MIGRATION_STATUS_PROPERTY_KEY);
		if (status == null) {
			return UNKNOWN;
		}
		return JobStatus.valueOf(status);
	}

	@Override
	public Branch getBranch() {
		return outV().nextOrDefaultExplicit(BranchImpl.class, null);
	}

	@Override
	public String getJobUuid() {
		return property(JOB_UUID_PROPERTY_KEY);
	}

	@Override
	public void setJobUuid(String uuid) {
		property(JOB_UUID_PROPERTY_KEY, uuid);
	}

	@Override
	public boolean isActive() {
		return property(ACTIVE_PROPERTY_KEY);
	}

	@Override
	public void setActive(boolean active) {
		property(ACTIVE_PROPERTY_KEY, active);
	}

}
