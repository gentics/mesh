package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

public class DateGraphFieldImpl extends AbstractBasicField<DateField>implements DateGraphField {

	public DateGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setDate(String date) {
		setFieldProperty("date", date);
	}

	public String getDate() {
		return getFieldProperty("date");
	}

	@Override
	public DateField transformToRest(ActionContext ac) {
		DateFieldImpl dateField = new DateFieldImpl();
		dateField.setDate(getDate());
		return dateField;
	}
}
