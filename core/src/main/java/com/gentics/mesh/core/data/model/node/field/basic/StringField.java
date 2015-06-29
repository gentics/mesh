package com.gentics.mesh.core.data.model.node.field.basic;

import com.gentics.mesh.core.data.model.node.field.nesting.ListableField;

public interface StringField extends ListableField, BasicField {

	String getString();

	void setString(String string);


}
