package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class BooleanField extends AbstractSimpleField {

	public BooleanField(String fieldKey, MeshNodeFieldContainer parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setBoolean(Boolean bool) {
		if (bool == null) {
			setFieldProperty("boolean", null);
		} else {
			setFieldProperty("boolean", String.valueOf(bool));
		}
	}

	public Boolean getBoolean() {
		return Boolean.valueOf(getFieldProperty("boolean"));
	}

}
