package com.gentics.mesh.rest.client;

import org.codehaus.jettison.json.JSONObject;

public class EventbusEvent {
	private final String address;
	private final Object body;

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

	public JSONObject getBodyAsJson() {
		if (body instanceof JSONObject) {
			return (JSONObject) body;
		} else {
			return null;
		}
	}
}
