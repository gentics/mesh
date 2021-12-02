package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.StringField;

public interface HibStringField extends HibListableField, HibBasicField<StringField>, HibDisplayField {

	/**
	 * Return the graph string value.
	 * 
	 * @return
	 */
	String getString();

	/**
	 * Set the string graph field value.
	 * 
	 * @param string
	 */
	void setString(String string);
}
