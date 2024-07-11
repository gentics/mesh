package com.gentics.mesh.core.data.node.field;

import java.util.Objects;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.StringFieldModel;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.handler.ActionContext;

public interface StringField extends ListableField, BasicField<StringFieldModel>, DisplayField {

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
	default Field cloneTo(FieldContainer container) {
		StringField clone = container.createString(getFieldKey());
		clone.setString(getString());
		return clone;
	}

	@Override
	default String getDisplayName() {
		return getString();
	}

	@Override
	default StringFieldModel transformToRest(ActionContext ac) {
		StringFieldImpl stringField = new StringFieldImpl();
		String text = getString();
		stringField.setString(text == null ? "" : text);
		return stringField;
	}

	default boolean stringEquals(Object obj) {
		if (obj instanceof StringField) {
			String valueA = getString();
			String valueB = ((StringField) obj).getString();
			return Objects.equals(valueA, valueB);
		}
		if (obj instanceof StringFieldModel) {
			String valueA = getString();
			String valueB = ((StringFieldModel) obj).getString();
			return Objects.equals(valueA, valueB);
		}
		return false;
	}
}
