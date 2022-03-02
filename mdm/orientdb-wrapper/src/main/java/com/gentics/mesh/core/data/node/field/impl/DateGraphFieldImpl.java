package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see DateGraphField
 */
public class DateGraphFieldImpl extends AbstractBasicField<DateField> implements DateGraphField {

	public DateGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setDate(Long date) {
		if (date == null) {
			setFieldProperty("date", null);
		} else {
			setFieldProperty("date", String.valueOf(date));
		}
	}

	@Override
	public Long getDate() {
		String value = getFieldProperty("date");
		if (value == null) {
			return null;
		} else {
			return Long.valueOf(value);
		}
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		setFieldProperty("date", null);
		setFieldKey(null);
	}

	@Override
	public boolean equals(Object obj) {
		return dateEquals(obj);
	}
}
