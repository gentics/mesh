package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.StringField;

public interface StringGraphField extends ListableGraphField, BasicGraphField<StringField> {

	String getString();

	void setString(String string);

}
