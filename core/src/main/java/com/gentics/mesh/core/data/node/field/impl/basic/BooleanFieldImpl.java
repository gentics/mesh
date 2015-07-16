package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.BasicField;
import com.gentics.mesh.core.data.node.field.basic.BooleanField;
import com.syncleus.ferma.AbstractVertexFrame;

public class BooleanFieldImpl extends AbstractBasicField implements BooleanField, BasicField {

	public BooleanFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setBoolean(Boolean bool) {
		if (bool == null) {
			setFieldProperty("boolean", null);
		} else {
			setFieldProperty("boolean", String.valueOf(bool));
		}
	}

	@Override
	public Boolean getBoolean() {
		String fieldValue = getFieldProperty("boolean");
		if (fieldValue == null) {
			return null;
		}
		return Boolean.valueOf(fieldValue);
	}

}
