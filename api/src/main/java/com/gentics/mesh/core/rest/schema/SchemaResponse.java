package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

public class SchemaResponse extends SchemaImpl implements RestResponse {

	private List<ProjectResponse> projects = new ArrayList<>();

	private String uuid;

	private String[] permissions = {};

	public SchemaResponse() {
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	public List<ProjectResponse> getProjects() {
		return projects;
	}

	public void setProjects(List<ProjectResponse> projects) {
		this.projects = projects;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}

}
