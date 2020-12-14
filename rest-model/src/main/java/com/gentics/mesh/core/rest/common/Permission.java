package com.gentics.mesh.core.rest.common;

/**
 * REST model for permissions.
 */
public enum Permission {

	CREATE("create"),

	READ("read"),

	UPDATE("update"),

	DELETE("delete"),

	READ_PUBLISHED("readpublished"),

	PUBLISH("publish");

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

	/**
	 * Convert the human readable permission name back into a graph permission object.
	 * 
	 * @param name
	 * @return
	 */
	public static Permission valueOfName(String name) {
		for (Permission p : Permission.values()) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}

	public static Permission[] basicPermissions() {
		return new Permission[] {CREATE, READ, UPDATE, DELETE};
	}
}
