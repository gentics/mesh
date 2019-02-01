package com.gentics.mesh.rest.client.impl;

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
