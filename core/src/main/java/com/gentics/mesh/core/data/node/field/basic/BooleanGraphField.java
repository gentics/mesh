package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public interface BooleanGraphField extends ListableGraphField, BasicGraphField {

	Boolean getBoolean();

	void setBoolean(Boolean bool);

}
