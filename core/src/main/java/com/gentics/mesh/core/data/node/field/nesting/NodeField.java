package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.MeshNode;

public interface NodeField extends ListableField, MicroschemaListableField {

	MeshNode getNode();

	void setFieldKey(String key);

}
