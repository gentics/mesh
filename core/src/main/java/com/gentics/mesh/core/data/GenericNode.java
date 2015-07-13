package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public interface GenericNode<T extends AbstractRestModel> extends MeshVertex, TransformableNode<T> {

	void setCreator(User user);

	User getCreator();

	List<? extends Project> getProjects();

	User getEditor();

	Long getLastEditedTimestamp();

	void setLastEditedTimestamp(long timestamp);

	void setEditor(User user);

	void setCreationTimestamp(long timestamp);

	Long getCreationTimestamp();

}
