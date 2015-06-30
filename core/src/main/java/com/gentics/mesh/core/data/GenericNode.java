package com.gentics.mesh.core.data;

import java.util.List;

public interface GenericNode extends MeshVertex {

	void setCreator(MeshUser user);

	MeshUser getCreator();

	void removeProject(Project project);

	void addProject(Project project);

	List<? extends Project> getProjects();

}
