package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Basic rest model abstract class for most mesh rest POJOs.
 *
 */
public class AbstractGenericNodeRestModel extends AbstractResponse {

	private UserReference creator;

	private long created;

	private UserReference editor;
	private long edited;

	private String[] permissions = {};

	/**
	 * 
	 * Return the creator user reference.
	 * 
	 * @return
	 */
	public UserReference getCreator() {
		return creator;
	}

	/**
	 * Set the creator user reference.
	 * 
	 * @param creator
	 */
	public void setCreator(UserReference creator) {
		this.creator = creator;
	}

	/**
	 * Return the creation date.
	 * 
	 * @return
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * Set the creation date.
	 * 
	 * @param created
	 */
	public void setCreated(long created) {
		this.created = created;
	}

	/**
	 * Return the editor user reference.
	 * 
	 * @return
	 */
	public UserReference getEditor() {
		return editor;
	}

	/**
	 * Set the editor user reference.
	 * 
	 * @param editor
	 */
	public void setEditor(UserReference editor) {
		this.editor = editor;
	}

	/**
	 * Return the last edited date.
	 * 
	 * @return
	 */
	public long getEdited() {
		return edited;
	}

	/**
	 * Set the last edited date.
	 * 
	 * @param edited
	 */
	public void setEdited(long edited) {
		this.edited = edited;
	}

	/**
	 * Return human readable permissions for the element.
	 * 
	 * @return
	 */
	public String[] getPermissions() {
		return permissions;
	}

	/**
	 * Set the human readable permission names for the element.
	 * 
	 * @param permissions
	 */
	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}
}
