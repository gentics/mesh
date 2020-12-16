package com.gentics.mesh.rest.client.impl;

/**
 * Eventbus message types for client side event handling.
 * Details on the event handling can be found in the Vert.x documentation.
 */
public enum EventbusMessageType {

	SEND("send"),
	PUBLISH("publish"),
	RECEIVE("receive"),
	REGISTER("register"),
	UNREGISTER("unregister"),
	PING("ping");

	public final String type;

	EventbusMessageType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return type;
	}
}
