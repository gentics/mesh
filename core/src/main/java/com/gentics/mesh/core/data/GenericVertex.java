package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;

public interface GenericVertex<T extends RestModel> extends MeshVertex, TransformableNode<T> {

	void setCreator(User user);

	User getCreator();

	User getEditor();

	Long getLastEditedTimestamp();

	void setLastEditedTimestamp(long timestamp);

	void setEditor(User user);

	void setCreationTimestamp(long timestamp);

	Long getCreationTimestamp();

	void delete();

}
