package com.gentics.mesh.core.rest.node.response.field;

public class DateFieldProperty extends AbstractFieldProperty {

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
		return PropertyFieldTypes.DATE.toString();
	}
}
