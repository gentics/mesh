package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.StringField;

public class StringFieldImpl implements StringField {

	private String string;

	@Override
	public String getString() {
		return string;
	}

	@Override
	public void setString(String text) {
		this.string = text;
	}

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

}
