package com.gentics.mesh.core.rest.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.Permission;
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
	@JsonPropertyDescription("Flag which indicates whether the permission update should be applied recursively.")
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
	 * @return Fluent API
	 */
	public RolePermissionRequest setRecursive(Boolean recursive) {
		this.recursive = recursive;
		return this;
	}

	/**
	 * Creates a {@link RolePermissionRequest} that is non-recursive and only has the given permissions set to true.
	 * All other permissions are set to false.
	 * @param permissions
	 * @return
	 */
	public static RolePermissionRequest withPermissions(Permission... permissions) {
		RolePermissionRequest rolePermissionRequest = new RolePermissionRequest();
		PermissionInfo permissionsInfo = rolePermissionRequest.getPermissions();
		for (Permission permission : permissions) {
			permissionsInfo.set(permission, true);
		}
		permissionsInfo.setOthers(false);
		return rolePermissionRequest;
	}

	/**
	 * Creates a {@link RolePermissionRequest} that is non-recursive and grants all permissions.
	 * @return
	 */
	public static RolePermissionRequest grantAll() {
		return withPermissions(Permission.values());
	}
}
