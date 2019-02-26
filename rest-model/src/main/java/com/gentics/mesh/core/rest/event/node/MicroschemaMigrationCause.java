package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;

public class MicroschemaMigrationCause extends MicroschemaMigrationMeshEventModel implements EventCauseInfo {

	/**
	 * Uuid of the migration job.
	 */
	private String uuid;

	public MicroschemaMigrationCause() {
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the job uuid.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.SCHEMA_MIGRATION;
	}
}
