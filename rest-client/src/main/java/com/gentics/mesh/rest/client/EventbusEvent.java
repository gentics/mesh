package com.gentics.mesh.rest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.json.JsonUtil;

import java.io.IOException;

/**
 * Represents an event from the Mesh eventbus.
 */
public class EventbusEvent {
	private final String address;
	private final JsonNode body;

	/**
	 * Parses a websocket text frame.
	 * @param rawText
	 * @throws IOException
	 */
	public EventbusEvent(String rawText) throws IOException {
		ObjectNode parsed = (ObjectNode) JsonUtil.getMapper().readTree(rawText);
		address = parsed.get("address").textValue();
		body = parsed.get("body");
	}

	/**
	 * Get the address of the event
	 * @return
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Get the body of the event
	 * @return
	 */
	public Object getBody() {
		return body;
	}

	/**
	 * Tries to get the body of the event as a string. Returns null if the body is not a string.
	 * @return
	 */
	public String getBodyAsString() {
		if (body.isTextual()) {
			return body.textValue();
		} else {
			return null;
		}
	}

	/**
	 * Tries to get the body of the event as a Json object. Returns null if the body is not an object.
	 * @return
	 */
	public ObjectNode getBodyAsJson() {
		if (body instanceof ObjectNode) {
			return (ObjectNode) body;
		} else {
			return null;
		}
	}
}
