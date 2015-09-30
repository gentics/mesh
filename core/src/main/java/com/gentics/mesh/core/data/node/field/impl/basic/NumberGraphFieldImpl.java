package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

public class NumberGraphFieldImpl extends AbstractBasicField<NumberField>implements NumberGraphField {

	public NumberGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setNumber(String number) {
		setFieldProperty("number", number);
	}

	@Override
	public String getNumber() {
		return getFieldProperty("number");
	}

	@Override
	public NumberField transformToRest(ActionContext ac) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return restModel;
	}
}
