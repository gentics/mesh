package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.node.field.DateField;

public class DateFieldImpl implements DateField {

	private String date;

	//TODO: decide on any special config properties for date type. TODO: Maybe a default timeframe would be useful.

	@Override
	public String getDate() {
		return date;
	}

	@Override
	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}
}
