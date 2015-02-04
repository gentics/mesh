package com.gentics.cailun.core.rest.model.auth.basic;

public enum BasicPermissionRights {
	READ("read"), WRITE("write"), DELETE("delete"), CREATE("create");

	private String propertyName;

	private BasicPermissionRights(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

}
