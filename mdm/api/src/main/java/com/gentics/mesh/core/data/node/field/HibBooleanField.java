package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.handler.ActionContext;

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
}
