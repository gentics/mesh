package com.gentics.cailun.core.data.model.auth;

public enum PermissionType {
	READ, UPDATE, DELETE, CREATE;

	public String getPropertyName() {
		return name().toLowerCase();
	}
}