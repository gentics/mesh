package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReference;

public interface GenericRestResponse extends RestResponse {

	/**
	 * Return the creator user reference.
	 * 
	 * @return Creator user reference
	 */
	UserReference getCreator();

	/**
	 * Set the creator user reference.
	 * 
	 * @param creator
	 */
	void setCreator(UserReference creator);

	/**
	 * Return the creation date in an ISO-8601 format.
	 * 
	 * @return Creation date
	 */
	String getCreated();

	/**
	 * Set the creation date in an ISO-8601 format.
	 * 
	 * @param created
	 *            Creation date
	 */
	void setCreated(String created);

	/**
	 * Return the editor user reference.
	 * 
	 * @return Editor user reference.
	 */
	UserReference getEditor();

	/**
	 * Set the editor user reference.
	 * 
	 * @param editor
	 */
	void setEditor(UserReference editor);

	/**
	 * Return the last edited date in an ISO-8601 format.
	 * 
	 * @return
	 */
	String getEdited();

	/**
	 * Set the last edited date in an ISO-8601 format.
	 * 
	 * @param edited
	 */
	void setEdited(String edited);

	/**
	 * Return human readable permissions for the element.
	 * 
	 * @return Array of human readable permissions
	 */
	String[] getPermissions();

	/**
	 * Set the human readable permission names for the element.
	 * 
	 * @param permissions
	 *            Human readable permissions
	 */
	void setPermissions(String... permissions);

	/**
	 * Return the human readable role permissions for the element.
	 * 
	 * @return
	 */
	String[] getRolePerms();

	/**
	 * Set the human readable role permissions for the element.
	 * 
	 * @param rolePerms
	 */
	void setRolePerms(String... rolePerms);

}
