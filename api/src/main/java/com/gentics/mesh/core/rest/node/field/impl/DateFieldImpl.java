package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.DateField;

/**
 * @see DateField
 */
public class DateFieldImpl implements DateField {

	@JsonPropertyDescription("ISO8601 formatted date field value")
	private String date;

	@Override
	public String getDate() {
		return date;
	}

	@Override
	public DateField setDate(String date) {
		this.date = date;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}

	@Override
	public String toString() {
		return String.valueOf(getDate());
	}
}
