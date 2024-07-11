package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.DateFieldModel;

/**
 * @see DateFieldModel
 */
public class DateFieldImpl implements DateFieldModel {

	@JsonPropertyDescription("ISO8601 formatted date field value")
	private String date;

	@Override
	public String getDate() {
		return date;
	}

	@Override
	public DateFieldModel setDate(String date) {
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
