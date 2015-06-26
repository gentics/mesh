package com.gentics.mesh.core.rest.node.response.field;

import com.gentics.mesh.model.FieldTypes;

public class DateField extends AbstractField {

	private String date;

	//TODO: decide on any special config properties for date type. TODO: Maybe a default timeframe would be useful.

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}
}
