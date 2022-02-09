package com.gentics.mesh.core.data.node.field;

import java.util.Objects;

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

	default boolean stringEquals(Object obj) {
		if (obj instanceof HibStringField) {
			String valueA = getString();
			String valueB = ((HibStringField) obj).getString();
			return Objects.equals(valueA, valueB);
		}
		if (obj instanceof StringField) {
			String valueA = getString();
			String valueB = ((StringField) obj).getString();
			return Objects.equals(valueA, valueB);
		}
		return false;
	}
}
