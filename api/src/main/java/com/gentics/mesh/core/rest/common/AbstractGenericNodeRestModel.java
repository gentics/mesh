package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReference;

public class AbstractGenericNodeRestModel extends AbstractRestModel {

	private UserReference creator;

	private long created;

	private UserReference editor;
	private long edited;

	private String[] permissions = {};

	public String[] getPermissions() {
		return permissions;
	}

	public UserReference getCreator() {
		return creator;
	}

	public long getCreated() {
		return created;
	}

	public UserReference getEditor() {
		return editor;
	}

	public long getEdited() {
		return edited;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public void setCreator(UserReference creator) {
		this.creator = creator;
	}

	public void setEdited(long edited) {
		this.edited = edited;
	}

	public void setEditor(UserReference editor) {
		this.editor = editor;
	}

	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}
}
