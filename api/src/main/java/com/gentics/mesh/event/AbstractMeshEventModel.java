package com.gentics.mesh.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractMeshEventModel implements MeshEventModel {

	private String uuid;

	private String name;

	private String origin;

	@JsonIgnore
	private String address;

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

}
