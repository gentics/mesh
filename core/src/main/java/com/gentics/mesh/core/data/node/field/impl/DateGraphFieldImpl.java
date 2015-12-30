package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Observable;

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
	public Observable<DateField> transformToRest(ActionContext ac) {
		DateFieldImpl dateField = new DateFieldImpl();
		dateField.setDate(getDate());
		return Observable.just(dateField);
	}
}
