package com.gentics.mesh.core.data.model.node.field.impl;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.AbstractSimpleField;
import com.gentics.mesh.core.data.model.node.field.DateField;

public class DateFieldImpl extends AbstractSimpleField implements DateField{

	public DateFieldImpl(String fieldKey, MeshNodeFieldContainerImpl parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setDate(String date) {
		setFieldProperty("date", date);
	}

	public String getDate() {
		return getFieldProperty("date");
	}
}
