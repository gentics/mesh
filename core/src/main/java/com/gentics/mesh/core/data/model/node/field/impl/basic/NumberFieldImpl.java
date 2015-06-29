package com.gentics.mesh.core.data.model.node.field.impl.basic;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.model.node.field.basic.NumberField;

public class NumberFieldImpl extends AbstractBasicField implements NumberField {

	public NumberFieldImpl(String fieldKey, AbstractFieldContainerImpl parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setNumber(String number) {
		setFieldProperty("number", number);
	}

	public String getNumber() {
		return getFieldProperty("number");
	}

}
