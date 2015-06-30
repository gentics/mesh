package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;

public interface ProjectRoot extends MeshVertex {

	Project create(String projectName);

	void addProject(Project project);

	List<? extends Project> getProjects();

	ProjectRootImpl getImpl();

}
