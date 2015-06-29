package com.gentics.mesh.core.rest.schema.response;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;

public class SchemaResponse extends AbstractRestModel {

	private List<ProjectResponse> projects = new ArrayList<>();

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

}
