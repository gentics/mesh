package com.gentics.mesh.core.data.node.field.impl.basic;

import java.text.NumberFormat;
import java.text.ParseException;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class NumberGraphFieldImpl extends AbstractBasicField<NumberField>implements NumberGraphField {

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
	public void transformToRest(ActionContext ac, Handler<AsyncResult<NumberField>> handler) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		handler.handle(Future.succeededFuture(restModel));
	}
}
