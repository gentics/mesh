package com.gentics.mesh.core.rest.role;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.rest.common.RestModel;

public class RolePermissionResponse implements RestModel {

	private Set<String> permissions = new HashSet<>();

	public Set<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}
}
