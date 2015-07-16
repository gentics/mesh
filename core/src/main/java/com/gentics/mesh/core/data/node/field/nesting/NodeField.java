package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;

public interface NodeField extends ListableReferencingField, MicroschemaListableField {

	Node getNode();

	void setFieldKey(String key);

}
