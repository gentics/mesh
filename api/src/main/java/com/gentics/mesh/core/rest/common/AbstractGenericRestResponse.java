package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Basic rest model abstract class for most mesh rest POJOs.
 */
public class AbstractGenericRestResponse extends AbstractResponse {

	private UserReference creator;

	private String created;

	private UserReference editor;

	private String edited;

	private String[] permissions = {};

	private String[] rolePerms;

	/**
	 * Return the creator user reference.
	 * 
	 * @return Creator user reference
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
	 * Return the creation date in an ISO-8601 format.
	 * 
	 * @return Creation date
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * Set the creation date in an ISO-8601 format.
	 * 
	 * @param created
	 *            Creation date
	 */
	public void setCreated(String created) {
		this.created = created;
	}

	/**
	 * Return the editor user reference.
	 * 
	 * @return Editor user reference.
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
	 * Return the last edited date in an ISO-8601 format.
	 * 
	 * @return
	 */
	public String getEdited() {
		return edited;
	}

	/**
	 * Set the last edited date in an ISO-8601 format.
	 * 
	 * @param edited
	 */
	public void setEdited(String edited) {
		this.edited = edited;
	}

	/**
	 * Return human readable permissions for the element.
	 * 
	 * @return Array of human readable permissions
	 */
	public String[] getPermissions() {
		return permissions;
	}

	/**
	 * Set the human readable permissions names for the element.
	 * 
	 * @param permissions
	 *            Human readable permissions
	 */
	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	/**
	 * Return the human readable role permissions for the element.
	 * 
	 * @return
	 */
	public String[] getRolePerms() {
		return rolePerms;
	}

	/**
	 * Set the human readable role permissions for the element.
	 * 
	 * @param rolePerms
	 */
	public void setRolePerms(String... rolePerms) {
		this.rolePerms = rolePerms;
	}

}
