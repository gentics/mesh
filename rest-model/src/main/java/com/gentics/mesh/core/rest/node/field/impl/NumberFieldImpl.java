package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NumberFieldModel;

/**
 * @see NumberFieldModel
 */
public class NumberFieldImpl implements NumberFieldModel {

	@JsonPropertyDescription("Number field value")
	private Number number;

	@Override
	public Number getNumber() {
		return number;
	}

	@Override
	public NumberFieldModel setNumber(Number number) {
		this.number = number;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.NUMBER.toString();
	}

	@Override
	public String toString() {
		return String.valueOf(getNumber());
	}

}
