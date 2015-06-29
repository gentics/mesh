package com.gentics.mesh.core.data.model.node.field.nesting;

import com.gentics.mesh.core.data.model.node.MeshNode;

public interface NodeField extends ListableField {

	MeshNode getNode();

	void setFieldKey(String key);

}
