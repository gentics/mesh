package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;

public class BranchMigrationCause extends BranchMigrationMeshEventModel implements EventCauseInfo {

	public BranchMigrationCause() {
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.BRANCH_MIGRATION;
	}
}
