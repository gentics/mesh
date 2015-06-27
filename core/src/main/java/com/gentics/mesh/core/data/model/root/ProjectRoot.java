package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.root.impl.ProjectRootImpl;

public interface ProjectRoot extends MeshVertex {

	Project create(String projectName);

	void addProject(Project project);

	List<? extends Project> getProjects();

	ProjectRootImpl getImpl();

}
