package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public interface StringField extends ListableField, BasicField {

	String getString();

	void setString(String string);


}
