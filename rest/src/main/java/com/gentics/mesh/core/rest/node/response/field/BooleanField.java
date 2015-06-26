package com.gentics.mesh.core.rest.node.response.field;

import com.gentics.mesh.model.FieldTypes;

public class BooleanField extends AbstractField {

	private Boolean value;

	public Boolean getValue() {
		return value;
	}

	public void setValue(Boolean value) {
		this.value = value;
	}

	@Override
	public String getType() {
		return FieldTypes.BOOLEAN.toString();
	}
}
