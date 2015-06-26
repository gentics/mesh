package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class DateFieldProperty extends AbstractSimpleFieldProperty {

	public DateFieldProperty(String fieldKey, MeshNodeFieldContainer parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setDate(String date) {
		setFieldProperty("date", date);
	}

	public String getDate() {
		return getFieldProperty("date");
	}
	
}
