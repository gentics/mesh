package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

public class BooleanGraphFieldImpl extends AbstractBasicField<BooleanField>implements BooleanGraphField {

	public BooleanGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setBoolean(Boolean bool) {
		if (bool == null) {
			setFieldProperty("boolean", "null");
		} else {
			setFieldProperty("boolean", String.valueOf(bool));
		}
	}

	@Override
	public Boolean getBoolean() {
		String fieldValue = getFieldProperty("boolean");
		if (fieldValue == null || fieldValue.equals("null")) {
			return null;
		}
		return Boolean.valueOf(fieldValue);
	}

	@Override
	public BooleanField transformToRest(ActionContext ac) {
		BooleanFieldImpl restModel = new BooleanFieldImpl();
		restModel.setValue(getBoolean());
		return restModel;
	}

}
