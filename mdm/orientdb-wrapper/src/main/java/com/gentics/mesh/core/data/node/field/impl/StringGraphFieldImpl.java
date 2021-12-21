package com.gentics.mesh.core.data.node.field.impl;

import java.util.Objects;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see StringGraphField
 */
public class StringGraphFieldImpl extends AbstractBasicField<StringField> implements StringGraphField {

	public StringGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setString(String string) {
		setFieldProperty("string", string);
	}

	@Override
	public String getString() {
		return getFieldProperty("string");
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		setFieldProperty("string", null);
		setFieldKey(null);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringGraphField) {
			String valueA = getString();
			String valueB = ((StringGraphField) obj).getString();
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
