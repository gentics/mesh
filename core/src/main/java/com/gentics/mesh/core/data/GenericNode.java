package com.gentics.mesh.core.data;

import java.util.List;

public interface GenericNode extends MeshVertex {

	void setCreator(User user);

	User getCreator();

	void removeProject(Project project);

	void addProject(Project project);

	List<? extends Project> getProjects();

	User getEditor();

	Long getLastEditedTimestamp();

	void setLastEditedTimestamp(long timestamp);

	void setEditor(User user);

	void setCreationTimestamp(long timestamp);

	Long getCreationTimestamp();

}
