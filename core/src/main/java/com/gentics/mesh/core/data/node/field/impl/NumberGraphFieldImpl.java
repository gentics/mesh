package com.gentics.mesh.core.data.node.field.impl;

import java.text.NumberFormat;
import java.text.ParseException;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Observable;

public class NumberGraphFieldImpl extends AbstractBasicField<NumberField> implements NumberGraphField {

	public NumberGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setNumber(Number number) {
		if (number == null) {
			setFieldProperty("number", null);
		} else {
			setFieldProperty("number", number.toString());
		}
	}

	@Override
	public Number getNumber() {
		String n = getFieldProperty("number");
		if (n == null) {
			return null;
		}

		try {
			return NumberFormat.getInstance().parse(n);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Observable<NumberField> transformToRest(ActionContext ac) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return Observable.just(restModel);
	}

	@Override
	public void removeField() {
		setFieldProperty("number", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		NumberGraphField clone = container.createNumber(getFieldKey());
		clone.setNumber(getNumber());
		return clone;
	}
}
