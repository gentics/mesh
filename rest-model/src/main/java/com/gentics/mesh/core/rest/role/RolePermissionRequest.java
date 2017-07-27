package com.gentics.mesh.core.rest.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a role permission request.
 *
 */
public class RolePermissionRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Set of permissions which should be applied.")
	private PermissionInfo permissions = new PermissionInfo();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the permission update should be applied recursivly.")
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
