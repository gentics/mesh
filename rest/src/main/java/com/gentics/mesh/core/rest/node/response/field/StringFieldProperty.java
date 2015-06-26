package com.gentics.mesh.core.rest.node.response.field;

public class StringFieldProperty extends AbstractFieldProperty {

	private String text;

	public StringFieldProperty(String text) {
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
		return PropertyFieldTypes.STRING.toString();
	}
}
