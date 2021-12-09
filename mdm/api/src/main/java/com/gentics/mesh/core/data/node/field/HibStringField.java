package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.handler.ActionContext;

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

	@Override
	default HibStringField cloneTo(HibFieldContainer container) {
		HibStringField clone = container.createString(getFieldKey());
		clone.setString(getString());
		return clone;
	}

	@Override
	default String getDisplayName() {
		return getString();
	}

	@Override
	default StringField transformToRest(ActionContext ac) {
		StringFieldImpl stringField = new StringFieldImpl();
		String text = getString();
		stringField.setString(text == null ? "" : text);
		return stringField;
	}
}
