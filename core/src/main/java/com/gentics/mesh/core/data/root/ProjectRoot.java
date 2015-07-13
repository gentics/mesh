package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public interface ProjectRoot extends RootVertex<Project, ProjectResponse> {

	Project create(String projectName);

	void removeProject(Project project);

	void addProject(Project project);

}
