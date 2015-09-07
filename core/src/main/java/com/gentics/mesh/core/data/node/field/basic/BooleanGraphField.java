package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;

public interface BooleanGraphField extends ListableGraphField, BasicGraphField<BooleanField> {

	Boolean getBoolean();

	void setBoolean(Boolean bool);

}
