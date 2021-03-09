package com.gentics.mesh.core.data.diff;

import java.util.Objects;

/**
 * This class can be used to construct a list of field container differences.
 */
public class FieldContainerChange {

	private String fieldKey;

	private String fieldCoordinates;

	private FieldChangeTypes type;

	public FieldContainerChange(String fieldKey, FieldChangeTypes type) {
		this.fieldKey = fieldKey;
		this.type = type;
		this.fieldCoordinates = fieldKey;
	}

	public String getFieldKey() {
		return fieldKey;
	}

	public FieldChangeTypes getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FieldContainerChange) {
			FieldContainerChange fcc = (FieldContainerChange) obj;
			if (Objects.equals(fcc.getFieldKey(), getFieldKey())) {
				return true;
			}
		}
		return super.equals(obj);
	}

	/**
	 * Set the fieldkey for this change.
	 * 
	 * @param fieldKey
	 */
	public void setFieldKey(String fieldKey) {
		this.fieldKey = fieldKey;
	}

	/**
	 * Return the field coordinates of the conflict. Micronodes are currently the only field which have dedicated field coordinates in the form of
	 * "[micronodeFieldKey].[nestedFieldKey]".
	 * 
	 * @return
	 */
	public String getFieldCoordinates() {
		return fieldCoordinates;
	}

	/**
	 * Set the field coordinates.
	 * 
	 * @param fieldCoordinates
	 */
	public void setFieldCoordinates(String fieldCoordinates) {
		this.fieldCoordinates = fieldCoordinates;
	}
}
