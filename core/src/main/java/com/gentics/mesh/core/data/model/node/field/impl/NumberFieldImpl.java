package com.gentics.mesh.core.data.model.node.field.impl;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.AbstractSimpleField;
import com.gentics.mesh.core.data.model.node.field.NumberField;

public class NumberFieldImpl extends AbstractSimpleField implements NumberField {

	public NumberFieldImpl(String fieldKey, MeshNodeFieldContainerImpl parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setNumber(String number) {
		setFieldProperty("number", number);
	}

	public String getNumber() {
		return getFieldProperty("number");
	}

}
