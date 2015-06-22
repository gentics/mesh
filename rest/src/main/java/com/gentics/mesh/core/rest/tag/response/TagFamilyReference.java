package com.gentics.mesh.core.rest.tag.response;

public class TagFamilyReference {

	private String name;
	private String uuid;

	public TagFamilyReference() {
	}

	public String getSchemaName() {
		return name;
	}

	public void setSchemaName(String name) {
		this.name = name;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
