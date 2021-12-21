package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see BooleanGraphField
 */
public class BooleanGraphFieldImpl extends AbstractBasicField<BooleanField> implements BooleanGraphField {

	public BooleanGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setBoolean(Boolean bool) {
		if (bool == null) {
			setFieldProperty("boolean", null);
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
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		setFieldProperty("boolean", null);
		setFieldKey(null);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanGraphField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanGraphField) obj).getBoolean();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof BooleanField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanField) obj).getValue();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}
