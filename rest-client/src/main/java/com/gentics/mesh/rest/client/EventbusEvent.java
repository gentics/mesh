package com.gentics.mesh.rest.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.json.JsonUtil;

import java.io.IOException;

public class EventbusEvent {
	private final String address;
	private final Object body;

	public EventbusEvent(String rawText) throws IOException {
		ObjectNode parsed = (ObjectNode) JsonUtil.getMapper().readTree(rawText);
		address = parsed.get("address").asText();
		body = parsed.get("body");
	}

	public EventbusEvent(String address, Object body) {
		this.address = address;
		this.body = body;
	}

	public String getAddress() {
		return address;
	}

	public Object getBody() {
		return body;
	}

	public String getBodyAsString() {
		if (body instanceof String) {
			return (String) body;
		} else {
			return null;
		}
	}

	public ObjectNode getBodyAsJson() {
		if (body instanceof ObjectNode) {
			return (ObjectNode) body;
		} else {
			return null;
		}
	}
}
