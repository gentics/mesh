package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.BooleanFieldModel;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

public interface BooleanField extends ListableField, BasicField<BooleanFieldModel> {

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
	default Field cloneTo(FieldContainer container) {
		BooleanField clone = container.createBoolean(getFieldKey());
		clone.setBoolean(getBoolean());
		return clone;
	}

	@Override
	default BooleanFieldModel transformToRest(ActionContext ac) {
		BooleanFieldImpl restModel = new BooleanFieldImpl();
		restModel.setValue(getBoolean());
		return restModel;
	}

	default boolean booleanEquals(Object obj) {
		if (obj instanceof BooleanField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanField) obj).getBoolean();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof BooleanFieldModel) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanFieldModel) obj).getValue();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}
