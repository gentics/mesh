package com.gentics.mesh.core.verticle.eventbus;

public enum EventbusAddress {

	MESH_MIGRATION("mesh.migration");

	private final String name;

	private EventbusAddress(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
