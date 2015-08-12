package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public interface StringGraphField extends ListableGraphField, BasicGraphField {

	String getString();

	void setString(String string);


}
