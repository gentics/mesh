package com.gentics.mesh.core.rest.common;

import java.util.HashSet;
import java.util.Set;

public class AbstractPermissionResponse implements RestModel {

	private Set<String> permissions = new HashSet<>();

	public Set<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}

}
