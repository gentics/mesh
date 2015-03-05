package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestProjectList {

	private List<RestProject> projects = new ArrayList<>();

	public RestProjectList() {
	}

	public List<RestProject> getProjects() {
		return projects;
	}

	public void addProject(RestProject project) {
		this.projects.add(project);
	}

}
