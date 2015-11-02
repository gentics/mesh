package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestResponse;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

/**
 * POJO for a schema response model.
 */
public class SchemaResponse extends SchemaImpl implements RestResponse {

	private String uuid;

	private String[] permissions = {};

	private String[] rolePerms;

	private List<ProjectReference> projects = new ArrayList<>();

	public SchemaResponse() {
	}

	/**
	 * Return the schema uuid.
	 * 
	 * @return Uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the schema uuid.
	 * 
	 * @param uuid
	 *            Uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the permissions of the schema.
	 * 
	 * @return Permissions
	 */
	public String[] getPermissions() {
		return permissions;
	}

	/**
	 * Set the permissions of the schema.
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

	/**
	 * Return a list of project references to which the schema was linked.
	 * 
	 * @return Project list
	 */
	public List<ProjectReference> getProjects() {
		return projects;
	}

	/**
	 * Set the list of project references to which the schema is linked.
	 * 
	 * @param projects
	 *            Project list
	 */
	public void setProjects(List<ProjectReference> projects) {
		this.projects = projects;
	}

}
