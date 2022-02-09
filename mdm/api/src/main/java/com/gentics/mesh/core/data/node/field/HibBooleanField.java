package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

public interface HibBooleanField extends HibListableField, HibBasicField<BooleanField> {

	/**
	 * Return the boolean field value.
	 * 
	 * @return
	 */
	Boolean getBoolean();

	/**
	 * Set the boolean field value.
	 * 
	 * @param bool
	 */
	void setBoolean(Boolean bool);

	@Override
	default HibBooleanField cloneTo(HibFieldContainer container) {
		HibBooleanField clone = container.createBoolean(getFieldKey());
		clone.setBoolean(getBoolean());
		return clone;
	}

	@Override
	default BooleanField transformToRest(ActionContext ac) {
		BooleanFieldImpl restModel = new BooleanFieldImpl();
		restModel.setValue(getBoolean());
		return restModel;
	}

	default boolean booleanEquals(Object obj) {
		if (obj instanceof HibBooleanField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((HibBooleanField) obj).getBoolean();
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
