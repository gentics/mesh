package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Information about the cause of an event. Events can be triggered by operations on other elements. (e.g. deleting a tag family results in tag deleted events).
 */
public interface EventCauseInfo extends RestModel {

	/**
	 * Return the type of the referenced element.
	 * 
	 * @return
	 */
	ElementType getType();

	/**
	 * Return the uuid of the referenced element.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Return the action for the cause.
	 * 
	 * @return
	 */
	EventCauseAction getAction();
}
