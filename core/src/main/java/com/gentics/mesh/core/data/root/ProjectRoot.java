package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;

public interface ProjectRoot extends RootVertex<Project> {

	Project create(String projectName);

	void removeProject(Project project);

	void addProject(Project project);

}
