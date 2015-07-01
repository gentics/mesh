package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.core.rest.user.UserResponse;

public class TagResponse extends AbstractRestModel {

	private TagFamilyReference tagFamily;

	private UserResponse creator;

	private long created;

	private UserResponse editor;
	private long edited;

	private String[] permissions = {};

	private TagFieldContainer fields = new TagFieldContainer();

	public TagResponse() {
	}

	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	public void setTagFamilyReference(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

	public TagFamilyReference getTagFamilyReference() {
		return tagFamily;
	}

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse author) {
		this.creator = author;
	}

	public UserResponse getEditor() {
		return editor;
	}

	public void setEditor(UserResponse editor) {
		this.editor = editor;
	}

	public long getEdited() {
		return edited;
	}

	public void setEdited(long edited) {
		this.edited = edited;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	public TagFieldContainer getFields() {
		return fields;
	}

}
