package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.RestResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaImpl;

public class MicroschemaResponse extends MicroschemaImpl implements RestResponse {
	private String uuid;

	private String[] permissions = {};

	private String[] rolePerms;

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the permissions of the element.
	 * 
	 * @return Permissions
	 */
	public String[] getPermissions() {
		return permissions;
	}

	/**
	 * Set the permissions of the element.
	 * 
	 * @param permissions
	 *            Permissions
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
