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
	 *            Permissions to be set to the affected elements. Omitted permissions will be revoked.
	 * @return Fluent API
	 */
	public RolePermissionRequest setPermissions(Set<String> permissions) {
		this.permissions = permissions;
		return this;
	}

	/**
	 * Flag that indicated that the request should be executed recursively.
	 * 
	 * @return Flag value
	 */
	public Boolean getRecursive() {
		return recursive;
	}

	/**
	 * Set the flag which indicated whether the permissions changes should be applied recursively.
	 * 
	 * @param recursive
	 *            Recursive flag value
	 * @return Fluent API
	 */
	public RolePermissionRequest setRecursive(Boolean recursive) {
		this.recursive = recursive;
		return this;
	}

}
