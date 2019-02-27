package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;

public enum EventCauseAction {
	SCHEMA_MIGRATION(SchemaMigrationCause.class),

	MICROSCHEMA_MIGRATION(MicroschemaMigrationCause.class),

	BRANCH_MIGRATION(BranchMigrationCause.class),

	DELETE(null);

	private final Class modelClass;

	<T extends EventCauseInfo> EventCauseAction(Class<T> modelClass) {
		this.modelClass = modelClass;
	}

	public <T extends EventCauseInfo> Class<T> getModelClass() {
		return modelClass;
	}
}
