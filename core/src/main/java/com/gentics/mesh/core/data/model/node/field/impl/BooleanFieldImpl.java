package com.gentics.mesh.core.data.model.node.field.impl;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.AbstractSimpleField;
import com.gentics.mesh.core.data.model.node.field.BooleanField;

public class BooleanFieldImpl extends AbstractSimpleField implements BooleanField{

	public BooleanFieldImpl(String fieldKey, MeshNodeFieldContainerImpl parentContainer) {
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
