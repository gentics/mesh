package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.event.migration.AbstractMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;

public enum EventCauseAction {
	SCHEMA_MIGRATION(SchemaMigrationMeshEventModel.class),

	MICROSCHEMA_MIGRATION(MicroschemaMigrationMeshEventModel.class),

	BRANCH_MIGRATION(BranchMigrationMeshEventModel.class),

	DELETE(null);

	private final Class modelClass;

	EventCauseAction(Class<? extends AbstractMigrationMeshEventModel> modelClass) {
		this.modelClass = modelClass;
	}
}