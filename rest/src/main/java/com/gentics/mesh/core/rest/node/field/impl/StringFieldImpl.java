package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.node.field.StringField;

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
