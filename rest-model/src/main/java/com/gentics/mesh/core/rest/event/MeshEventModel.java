package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

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

	/**
	 * Returns the event cause info which contains information about the root action which lead to the creation of this event.
	 *
	 * @return
	 */
	EventCauseInfo getCause();

	/**
	 * Set the cause info for the event.
	 *
	 * @param cause
	 */
	void setCause(EventCauseInfo cause);

	/**
	 * Gets the body of an eventbus message as a POJO.
	 * @param message
	 * @return
	 */
	static MeshEventModel fromMessage(Message<JsonObject> message) {
		String address = message.address();
		MeshEvent event = MeshEvent.fromAddress(address)
			.orElseThrow(() -> new RuntimeException(String.format("No event found for address %s", address)));

		// TODO Find better way to deserialize
		return (MeshEventModel) JsonUtil.readValue(message.body().toString(), event.bodyModel);
	}

}
