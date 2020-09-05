package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.rest.job.JobStatus;

public interface HibBranchVersionAssignment extends HibElement {

	String getJobUuid();

	boolean isActive();

	void setActive(boolean flag);

	JobStatus getMigrationStatus();

	void setMigrationStatus(JobStatus status);

}
