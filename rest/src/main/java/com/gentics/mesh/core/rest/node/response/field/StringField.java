package com.gentics.mesh.core.rest.node.response.field;

import com.gentics.mesh.model.FieldTypes;

public class StringField extends AbstractField {

	private String text;

	public StringField(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}
}
