package com.gentics.cailun.core.data.model.auth;

public enum PermissionType {
	READ, WRITE, DELETE, CREATE;

	public String getPropertyName() {
		return name().toLowerCase();
	}
}