package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;

public interface ProjectRoot extends RootVertex<Project> {

	Project create(String projectName, User creator);

	void removeProject(Project project);

	void addProject(Project project);

}
