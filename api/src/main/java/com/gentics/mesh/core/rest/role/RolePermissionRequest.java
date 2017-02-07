package com.gentics.mesh.core.rest.role;

import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a role permission request.
 *
 */
public class RolePermissionRequest implements RestModel {

	private PermissionInfo permissions = new PermissionInfo();

	private Boolean recursive = false;

	public RolePermissionRequest() {
	}

	/**
	 * Return a set of permissions that should be set to the affected elements.
	 * 
	 * @return
	 */
	public PermissionInfo getPermissions() {
		return permissions;
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
	 * Set the flag which indicated whether the permission changes should be applied recursively.
	 * 
	 * @param recursive
	 *            Recursive flag value
	 */
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

}
