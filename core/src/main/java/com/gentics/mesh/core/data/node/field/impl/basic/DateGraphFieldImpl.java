package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.syncleus.ferma.AbstractVertexFrame;

public class DateGraphFieldImpl extends AbstractBasicField implements DateGraphField{

	public DateGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setDate(String date) {
		setFieldProperty("date", date);
	}

	public String getDate() {
		return getFieldProperty("date");
	}
}
