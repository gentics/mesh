package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public interface BooleanField extends ListableField, BasicField {

	Boolean getBoolean();

	void setBoolean(Boolean bool);

}
