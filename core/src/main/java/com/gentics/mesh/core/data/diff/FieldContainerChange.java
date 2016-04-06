package com.gentics.mesh.core.data.diff;

/**
 * This class can be used to construct a list of field container differences.
 */
public class FieldContainerChange {

	String fieldName;

	FieldChangeTypes type;

	public FieldContainerChange(String fieldName, FieldChangeTypes type) {
		this.fieldName = fieldName;
		this.type = type;
	}

	public String getFieldName() {
		return fieldName;
	}

	public FieldChangeTypes getType() {
		return type;
	}
}
