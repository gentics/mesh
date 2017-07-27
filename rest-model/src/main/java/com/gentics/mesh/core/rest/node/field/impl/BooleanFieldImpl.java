package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BooleanField;

/**
 * @see BooleanField
 */
public class BooleanFieldImpl implements BooleanField {

	@JsonPropertyDescription("Boolean field value")
	private Boolean value;

	@Override
	public Boolean getValue() {
		return value;
	}

	@Override
	public BooleanField setValue(Boolean value) {
		this.value = value;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.BOOLEAN.toString();
	}

	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
}
