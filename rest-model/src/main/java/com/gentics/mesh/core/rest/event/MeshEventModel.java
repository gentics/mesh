package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * A mesh event model contains the information which is included in the event body payload.  
 */
public interface MeshEventModel extends RestModel {

	/**
	 * Return the mesh cluster node from which this event originated.
	 * 
	 * @return
	 */
	String getOrigin();

	/**
	 * Set the mesh cluster node from which the event originates.
	 * 
	 * @param origin
	 */
	void setOrigin(String origin);

	/**
	 * Return the event to which the model belongs.
	 * 
	 * @return
	 */
	MeshEvent getEvent();

	/**
	 * Set the event to which the model belongs.
	 * 
	 * @param address
	 */
	void setEvent(MeshEvent event);

	/**
	 * Returns the event cause info which contains information about the root action which lead to the event.
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
	 * 
	 * @param message
	 * @return
	 */
	static <T extends MeshEventModel> T fromMessage(Message<JsonObject> message) {
		String address = message.address();
		MeshEvent event = MeshEvent.fromAddress(address)
			.orElseThrow(() -> new RuntimeException(String.format("No event found for address %s", address)));

		JsonObject body = message.body();
		// TODO Find better way to deserialize
		if (body == null) {
			return null;
		} else {
			return (T) JsonUtil.readValue(body.toString(), event.bodyModel);
		}
	}

}
