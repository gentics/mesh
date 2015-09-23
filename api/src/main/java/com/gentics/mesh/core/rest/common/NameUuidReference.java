package com.gentics.mesh.core.rest.common;

public class NameUuidReference<T> {

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

	public T setName(String name) {
		this.name = name;
		return (T) this;
	}

	public String getUuid() {
		return uuid;
	}

	public T setUuid(String uuid) {
		this.uuid = uuid;
		return (T) this;
	}
}
