package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.common.RestModel;
import io.vertx.core.eventbus.Message;

public interface MeshEventModel extends RestModel {

	/**
	 * Return the uuid of the element which the element references
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Return the mesh cluster node from which this event originated.
	 * 
	 * @return
	 */

	String getOrigin();

	/**
	 * Return the name of the referenced element.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Return the address of the event.
	 * 
	 * @return
	 */
	String getAddress();

	static MeshEventModel fromMessage(Message<String> message) {
		return null;
	}
}
