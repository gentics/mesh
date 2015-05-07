package com.gentics.mesh.core.data.model.auth;

public enum PermissionType {
	READ, UPDATE, DELETE, CREATE;

	public String getPropertyName() {
		return name().toLowerCase();
	}

	public static PermissionType fromString(String text) {
		if (text != null) {
			for (PermissionType b : PermissionType.values()) {
				if (text.equalsIgnoreCase(b.name())) {
					return b;
				}
			}
		}
		return null;
	}
}