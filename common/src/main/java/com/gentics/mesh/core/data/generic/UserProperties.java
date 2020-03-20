package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;

public interface UserProperties {

	void setCreator(MeshVertex vertex, User user);

	void setEditor(MeshVertex vertex, User user);

	User getEditor(MeshVertex vertex);

	User getCreator(MeshVertex vertex);

}
