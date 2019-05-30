package com.gentics.mesh.core.schema.field;

import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.job.JobStatus;

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
	public MigrationStatusHandler error(Throwable error, String string) {
		return this;
	}

	@Override
	public void setVersionEdge(BranchVersionEdge versionEdge) {

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
