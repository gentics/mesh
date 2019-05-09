package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.ElementType.PROJECT;

import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.MeshEventModel;

public final class EventCauseHelper {

	private EventCauseHelper() {

	}

	/**
	 * Check whether the given message contains a cause which hints towards a project deletion.
	 * 
	 * @param message
	 * @return
	 */
	public static boolean isProjectDeleteCause(MeshEventModel message) {
		EventCauseInfo cause = message.getCause();
		return cause != null && cause.getAction() == EventCauseAction.DELETE && cause.getType() == PROJECT;
	}

}
