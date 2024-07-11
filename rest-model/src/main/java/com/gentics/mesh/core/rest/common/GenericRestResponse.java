package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.user.UserReferenceModel;

/**
 * Common interface for typical REST responses which return entity information.
 */
public interface GenericRestResponse extends RestResponse {

	/**
	 * Return the creator user reference.
	 * 
	 * @return Creator user reference
	 */
	UserReferenceModel getCreator();

	/**
	 * Set the creator user reference.
	 * 
	 * @param creator
	 */
	void setCreator(UserReferenceModel creator);

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
	UserReferenceModel getEditor();

	/**
	 * Set the editor user reference.
	 * 
	 * @param editor
	 */
	void setEditor(UserReferenceModel editor);

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
	PermissionInfoModel getPermissions();

	/**
	 * Set the permissions for the element.
	 * 
	 * @param permissions
	 */
	void setPermissions(PermissionInfoModel permissions);

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
	PermissionInfoModel getRolePerms();

	/**
	 * Set the role permissions for the element.
	 * 
	 * @param rolePerms
	 */
	void setRolePerms(PermissionInfoModel rolePerms);

	/**
	 * 
	 * Set specific granted role permissions for the element. Permissions which are not included will be set to false.
	 * 
	 * @param permissions
	 */
	void setRolePerms(Permission... permissions);

}
