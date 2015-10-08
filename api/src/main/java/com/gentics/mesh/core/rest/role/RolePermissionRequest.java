package com.gentics.mesh.core.rest.role;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a role permission request.
 *
 */
public class RolePermissionRequest implements RestModel {

	private Set<String> permissions = new HashSet<>();

	private Boolean recursive = false;

	public RolePermissionRequest() {
	}

	/**
	 * Return a set of permissions that should be set to the affected elements.
	 * 
	 * @return
	 */
	public Set<String> getPermissions() {
		return permissions;
	}

	/**
	 * Set a set of permissions that should be set to the affected elements.
	 * 
	 * @param permissions
	 */
	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}

	/**
	 * Flag that indicated that the request should be executed recursively.
	 * 
	 * @return
	 */
	public Boolean getRecursive() {
		return recursive;
	}

	/**
	 * Set the flag which indicated whether the permission changes should be applied recursively.
	 * 
	 * @param recursive
	 */
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

}
