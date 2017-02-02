package com.gentics.mesh.core.rest.common;

public enum Permission {

	CREATE_PERM("create"), READ_PERM("read"), UPDATE_PERM("update"), DELETE_PERM("delete");

	private String name;

	/**
	 * Create a new permission
	 * 
	 * @param name
	 *            human readable name
	 */
	Permission(String name) {
		this.name = name;
	}

	/**
	 * Return the human readable name of the permission.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

}
