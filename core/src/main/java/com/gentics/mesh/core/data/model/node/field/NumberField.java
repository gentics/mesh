package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class NumberField extends AbstractSimpleField {

	public NumberField(String fieldKey, MeshNodeFieldContainer parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setNumber(String number) {
		setFieldProperty("number", number);
	}

	public String getNumber() {
		return getFieldProperty("number");
	}

}
