package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.common.RestModel;

public interface EventCauseInfo extends RestModel {
	ElementType getType();

	String getUuid();

	EventCauseAction getAction();
}
