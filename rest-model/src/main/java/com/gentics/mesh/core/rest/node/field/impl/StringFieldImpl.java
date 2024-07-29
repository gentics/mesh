package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.StringField;

/**
 * @see StringField
 */
public class StringFieldImpl implements StringField {

	@JsonPropertyDescription("String field value")
	private String string;

	@Override
	public String getString() {
		return string;
	}

	@Override
	public StringField setString(String text) {
		this.string = text;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

}
