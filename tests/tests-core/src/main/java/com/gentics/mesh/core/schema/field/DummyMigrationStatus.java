package com.gentics.mesh.core.schema.field;

import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobWarningList;

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
	public MigrationStatusHandler commit() {
		return this;
	}

	@Override
	public MigrationStatusHandler done() {
		return this;
	}

	@Override
	public MigrationStatusHandler done(JobWarningList warningList) {
		return this;
	}

	@Override
	public MigrationStatusHandler error(Throwable error, String string) {
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
