package com.gentics.mesh.core.rest.event.node;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;

/**
 * Event info model for a mail sending cause.
 */
public class MailSendingCause implements EventCauseInfo {

	public MailSendingCause() {
	}

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
		return EventCauseAction.MAIL_SENDING;
	}
}
