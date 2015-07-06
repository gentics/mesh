package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

public class SchemaResponse extends SchemaImpl {

	private List<ProjectResponse> projects = new ArrayList<>();

	private String uuid;

	private String[] perms = {};

	public SchemaResponse() {
	}

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
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
