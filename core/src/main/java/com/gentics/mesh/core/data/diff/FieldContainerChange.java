package com.gentics.mesh.core.data.diff;

import java.util.Objects;

/**
 * This class can be used to construct a list of field container differences.
 */
public class FieldContainerChange {

	String fieldKey;

	FieldChangeTypes type;

	public FieldContainerChange(String fieldKey, FieldChangeTypes type) {
		this.fieldKey = fieldKey;
		this.type = type;
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
}
