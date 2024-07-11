package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfoModel;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;

/**
 * Event info model for a branch migration cause.
 */
public class BranchMigrationCause extends BranchMigrationMeshEventModel implements EventCauseInfoModel {

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
