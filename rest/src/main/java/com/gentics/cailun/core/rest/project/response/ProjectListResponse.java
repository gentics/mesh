package com.gentics.cailun.core.rest.project.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;

public class ProjectListResponse extends AbstractListResponse {

	private List<ProjectResponse> projects = new ArrayList<>();

	public ProjectListResponse() {
	}

	public List<ProjectResponse> getProjects() {
		return projects;
	}

	public void addProject(ProjectResponse project) {
		this.projects.add(project);
	}

}
