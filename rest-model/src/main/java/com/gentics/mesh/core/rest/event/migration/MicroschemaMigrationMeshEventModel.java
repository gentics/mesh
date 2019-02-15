package com.gentics.mesh.core.rest.event.migration;

import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class MicroschemaMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	private MicroschemaReference fromVersion;

	private MicroschemaReference toVersion;

	public MicroschemaMigrationMeshEventModel() {
	}

	public MicroschemaReference getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(MicroschemaReference fromVersion) {
		this.fromVersion = fromVersion;
	}

	public MicroschemaReference getToVersion() {
		return toVersion;
	}

	public void setToVersion(MicroschemaReference toVersion) {
		this.toVersion = toVersion;
	}
}
