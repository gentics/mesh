package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfoModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;

/**
 * Cause POJO for a schema migration cause that can be included in an event.
 */
public class SchemaMigrationCause extends SchemaMigrationMeshEventModel implements EventCauseInfoModel {

	public SchemaMigrationCause() {
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.SCHEMA_MIGRATION;
	}
}
