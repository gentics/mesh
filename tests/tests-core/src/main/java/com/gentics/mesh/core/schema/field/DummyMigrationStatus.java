package com.gentics.mesh.core.schema.field;

import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.job.JobStatus;

/**
 * Wrapper for migration status for tests.
 */
public class DummyMigrationStatus implements MigrationStatusHandler {

	private static DummyMigrationStatus instance = new DummyMigrationStatus();

	private DummyMigrationStatus() {

	}

	public static DummyMigrationStatus get() {
		return instance;
	}

	@Override
	public MigrationStatusHandler commit(HibJob job) {
		return this;
	}

	@Override
	public MigrationStatusHandler done(HibJob job) {
		return this;
	}

	@Override
	public MigrationStatusHandler error(HibJob job, Throwable error, String string) {
		return this;
	}

	@Override
	public void setVersionEdge(HibBranchVersionAssignment versionEdge) {

	}

	@Override
	public void setStatus(JobStatus status) {

	}

	@Override
	public void setCompletionCount(long completionCount) {

	}

	@Override
	public void incCompleted() {

	}
}
