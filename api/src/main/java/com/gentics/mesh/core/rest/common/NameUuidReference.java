package com.gentics.mesh.core.rest.common;

public class NameUuidReference {

	private String name;
	private String uuid;

	public NameUuidReference() {
	}

	public NameUuidReference(String name, String uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
