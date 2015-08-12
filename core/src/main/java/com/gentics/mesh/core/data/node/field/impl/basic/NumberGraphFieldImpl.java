package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.syncleus.ferma.AbstractVertexFrame;

public class NumberGraphFieldImpl extends AbstractBasicField implements NumberGraphField {

	public NumberGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setNumber(String number) {
		setFieldProperty("number", number);
	}

	public String getNumber() {
		return getFieldProperty("number");
	}

}
