package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.model.FieldTypes;

public class StringFieldImpl implements StringField {

	private String text;

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

}
