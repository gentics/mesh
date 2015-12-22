package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Observable;

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
	public Observable<StringField> transformToRest(ActionContext ac) {
		StringFieldImpl stringField = new StringFieldImpl();
		String text = getString();
		stringField.setString(text == null ? "" : text);
		return Observable.just(stringField);
	}

}
