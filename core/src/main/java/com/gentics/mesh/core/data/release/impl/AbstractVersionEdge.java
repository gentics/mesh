package com.gentics.mesh.core.data.release.impl;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.UNKNOWN;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.syncleus.ferma.AbstractEdgeFrame;

/**
 * Abstract implementation for {@link ReleaseMicroschemaEdgeImpl} and {@link ReleaseSchemaEdgeImpl}.
 */
public abstract class AbstractVersionEdge extends AbstractEdgeFrame implements ReleaseVersionEdge {

	public static final String MIGRATION_STATUS_PROPERTY_KEY = "migrationStatus";

	public static final String JOB_UUID_PROPERTY_KEY = "jobUuid";

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
	public Release getRelease() {
		return outV().nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public String getJobUuid() {
		return getProperty(JOB_UUID_PROPERTY_KEY);
	}

	@Override
	public void setJobUuid(String uuid) {
		setProperty(JOB_UUID_PROPERTY_KEY, uuid);
	}

}
