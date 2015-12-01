package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NumberField;

public class NumberFieldImpl implements NumberField {

	private Number number;

	@Override
	public Number getNumber() {
		return number;
	}

	@Override
	public NumberField setNumber(Number number) {
		this.number = number;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.NUMBER.toString();
	}

}
