package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;

public class SchemaMigrationCause extends SchemaMigrationMeshEventModel implements EventCauseInfo {

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public String getUuid() {
		return null;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.SCHEMA_MIGRATION;
	}
}
