package com.gentics.mesh.core.rest.role;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.rest.common.RestModel;

public class RolePermissionRequest implements RestModel {

	private Set<String> permissions = new HashSet<>();

	private Boolean recursive;

	public RolePermissionRequest() {
	}

	public Set<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}

	public Boolean getRecursive() {
		return recursive;
	}

	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

}
