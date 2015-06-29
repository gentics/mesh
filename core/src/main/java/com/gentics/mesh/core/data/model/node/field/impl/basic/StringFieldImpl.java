package com.gentics.mesh.core.data.model.node.field.impl.basic;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.model.node.field.basic.StringField;

public class StringFieldImpl extends AbstractBasicField implements StringField {

	public StringFieldImpl(String fieldKey, AbstractFieldContainerImpl parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setString(String string) {
		setFieldProperty("string", string);
	}

	@Override
	public String getString() {
		return getFieldProperty("string");
	}

}
