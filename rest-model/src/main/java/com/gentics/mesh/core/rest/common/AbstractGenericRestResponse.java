package com.gentics.mesh.core.rest.common;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.parameter.RolePermissionParameters;

/**
 * Basic rest model abstract class for most mesh rest POJOs.
 */
public abstract class AbstractGenericRestResponse extends AbstractResponse implements GenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("User reference of the creator of the element.")
	private UserReference creator;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO8601 formatted created date string.")
	private String created;

	@JsonProperty(required = false)
	@JsonPropertyDescription("User reference of the editor of the element.")
	private UserReference editor;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO8601 formatted edited date string.")
	private String edited;

	@JsonProperty(value = "permissions", required = true)
	private PermissionInfo permissions;

	@JsonPropertyDescription("Permission information for provided role. This property will only be populated if a "
			+ RolePermissionParameters.ROLE_PERMISSION_QUERY_PARAM_KEY + " query parameter has been specified.")
	@JsonProperty(value = "rolePerms", required = false)
	private PermissionInfo rolePerms;

	@Override
	public UserReference getCreator() {
		return creator;
	}

	@Override
	public void setCreator(UserReference creator) {
		this.creator = creator;
	}

	@Override
	public String getCreated() {
		return created;
	}

	@Override
	public void setCreated(String created) {
		this.created = created;
	}

	@Override
	public UserReference getEditor() {
		return editor;
	}

	@Override
	public void setEditor(UserReference editor) {
		this.editor = editor;
	}

	@Override
	public String getEdited() {
		return edited;
	}

	@Override
	public void setEdited(String edited) {
		this.edited = edited;
	}

	@Override
	public PermissionInfo getPermissions() {
		return permissions;
	}

	@Override
	public void setPermissions(PermissionInfo permissions) {
		this.permissions = permissions;
	}

	@Override
	@JsonIgnore
	public void setPermissions(Permission... permissions) {
		if (this.permissions == null) {
			this.permissions = new PermissionInfo();
		}
		for (Permission permission : Arrays.asList(permissions)) {
			getPermissions().set(permission, true);
		}
		getPermissions().setOthers(false);
	}

	@Override
	public PermissionInfo getRolePerms() {
		return rolePerms;
	}

	@Override
	public void setRolePerms(PermissionInfo rolePerms) {
		this.rolePerms = rolePerms;
	}

	@Override
	@JsonIgnore
	public void setRolePerms(Permission... permissions) {
		if (rolePerms == null) {
			rolePerms = new PermissionInfo();
		}
		for (Permission permission : Arrays.asList(permissions)) {
			rolePerms.set(permission, true);
		}
		rolePerms.setOthers(false);
	}

}
