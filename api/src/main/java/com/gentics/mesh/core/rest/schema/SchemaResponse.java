package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

/**
 * POJO for a schema response model.
 */
public class SchemaResponse extends SchemaImpl implements RestResponse {

	private String uuid;

	private String[] permissions = {};

	private List<ProjectResponse> projects = new ArrayList<>();

	public SchemaResponse() {
	}

	/**
	 * Return the schema uuid.
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the schema uuid.
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the permissions of the schema.
	 * 
	 * @return
	 */
	public String[] getPermissions() {
		return permissions;
	}

	/**
	 * Set the permissions of the schema.
	 * 
	 * @param permissions
	 */
	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	/**
	 * Return a list of projects to which the schema was linked.
	 * 
	 * @return
	 */
	public List<ProjectResponse> getProjects() {
		return projects;
	}

	/**
	 * Set the list of project to which the schema is linked.
	 * 
	 * @param projects
	 */
	public void setProjects(List<ProjectResponse> projects) {
		this.projects = projects;
	}

}
