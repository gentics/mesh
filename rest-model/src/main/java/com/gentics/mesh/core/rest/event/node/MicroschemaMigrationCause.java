package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;

/**
 * Cause POJO for a microschema migration cause that can be included in an event.
 */
public class MicroschemaMigrationCause extends MicroschemaMigrationMeshEventModel implements EventCauseInfo {

	public MicroschemaMigrationCause() {
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.MICROSCHEMA_MIGRATION;
	}
}
