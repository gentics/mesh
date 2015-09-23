package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.DateField;

public class DateFieldImpl implements DateField {

	private Long date;

	@Override
	public Long getDate() {
		return date;
	}

	@Override
	public DateField setDate(Long date) {
		this.date = date;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}
}
