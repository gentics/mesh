package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Common interface for typical REST responses which return entity information.
 */
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
	 * Return the permissions for the element.
	 * 
	 * @return
	 */
	PermissionInfo getPermissions();

	/**
	 * Set the permissions for the element.
	 * 
	 * @param permissions
	 */
	void setPermissions(PermissionInfo permissions);

	/**
	 * Set specific granted permissions for the element. Permissions which are not included will be set to false.
	 * 
	 * @param permissions
	 */
	void setPermissions(Permission... permissions);

	/**
	 * Return the role permissions for the element.
	 * 
	 * @return
	 */
	PermissionInfo getRolePerms();

	/**
	 * Set the role permissions for the element.
	 * 
	 * @param rolePerms
	 */
	void setRolePerms(PermissionInfo rolePerms);

	/**
	 * 
	 * Set specific granted role permissions for the element. Permissions which are not included will be set to false.
	 * 
	 * @param permissions
	 */
	void setRolePerms(Permission... permissions);

}
