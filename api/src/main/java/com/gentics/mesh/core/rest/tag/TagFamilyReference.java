package com.gentics.mesh.core.rest.tag;

public class TagFamilyReference {

	private String name;
	private String uuid;

	public TagFamilyReference() {
	}

	public String getName() {
		return name;
	}

	public TagFamilyReference setName(String name) {
		this.name = name;
		return this;
	}

	public String getUuid() {
		return uuid;
	}

	public TagFamilyReference setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

}
