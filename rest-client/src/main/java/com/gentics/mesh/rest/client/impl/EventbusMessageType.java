package com.gentics.mesh.rest.client.impl;

public enum EventbusMessageType {
	SEND("send"),
	PUBLISH("publish"),
	RECEIVE("receive"),
	REGISTER("register"),
	UNREGISTER("unregister");

	public final String type;

	EventbusMessageType(String type) {
		this.type = type;
	}
}
