package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Basic rest model abstract class for most mesh rest POJOs.
 */
public abstract class AbstractGenericRestResponse extends AbstractResponse implements GenericRestResponse {

	private UserReference creator;

	private String created;

	private UserReference editor;

	private String edited;

	private String[] permissions = {};

	private String[] rolePerms;

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
	public String[] getPermissions() {
		return permissions;
	}

	@Override
	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	@Override
	public String[] getRolePerms() {
		return rolePerms;
	}

	@Override
	public void setRolePerms(String... rolePerms) {
		this.rolePerms = rolePerms;
	}

}
