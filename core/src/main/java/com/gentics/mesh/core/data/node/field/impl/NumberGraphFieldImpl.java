package com.gentics.mesh.core.data.node.field.impl;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Single;

public class NumberGraphFieldImpl extends AbstractBasicField<NumberField> implements NumberGraphField {

	public NumberGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setNumber(Number number) {
		if (number == null) {
			setFieldProperty("number", null);
		} else {
			setFieldProperty("number", NumberFormat.getInstance(Locale.ENGLISH).format(number));
		}
	}

	@Override
	public Number getNumber() {
		String n = getFieldProperty("number");
		if (n == null) {
			return null;
		}

		try {
			return NumberFormat.getInstance(Locale.ENGLISH).parse(n);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Single<NumberField> transformToRest(ActionContext ac) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return Single.just(restModel);
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		setFieldProperty("number", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		NumberGraphField clone = container.createNumber(getFieldKey());
		clone.setNumber(getNumber());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumberGraphField) {
			Number valueA = getNumber();
			Number valueB = ((NumberGraphField) obj).getNumber();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof NumberField) {
			Number valueA = getNumber();
			Number valueB = ((NumberField) obj).getNumber();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}
