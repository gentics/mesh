package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.syncleus.ferma.AbstractVertexFrame;

public class DateFieldImpl extends AbstractBasicField implements DateField{

	public DateFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setDate(String date) {
		setFieldProperty("date", date);
	}

	public String getDate() {
		return getFieldProperty("date");
	}
}
