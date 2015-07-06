package com.gentics.mesh.core.rest.common;

public enum Permission {
	CREATE_PERM("create"), READ_PERM("read"), UPDATE_PERM("update"), DELETE_PERM("delete");

	private String name;

	Permission(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
