package com.gentics.mesh.core.data.node.field.impl;

import java.util.Objects;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Single;

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
	public Single<StringField> transformToRest(ActionContext ac) {
		StringFieldImpl stringField = new StringFieldImpl();
		String text = getString();
		stringField.setString(text == null ? "" : text);
		return Single.just(stringField);
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		setFieldProperty("string", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		StringGraphField clone = container.createString(getFieldKey());
		clone.setString(getString());
		return clone;
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
