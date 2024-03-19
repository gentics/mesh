package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see NumberGraphField
 */
public class NumberGraphFieldImpl extends AbstractBasicField<NumberField> implements NumberGraphField {

	public NumberGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setNumber(Number number) {
		setFieldProperty("number", number);
	}

	@Override
	public Number getNumber() {
		return getFieldProperty("number");
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		setFieldProperty("number", null);
		setFieldKey(null);
	}

	@Override
	public boolean equals(Object obj) {
		return numberEquals(obj);
	}
}
