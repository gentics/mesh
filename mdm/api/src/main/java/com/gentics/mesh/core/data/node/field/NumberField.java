package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.NumberFieldModel;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

public interface NumberField extends ListableField, BasicField<NumberFieldModel> {

	/**
	 * Set the number in the graph field.
	 * 
	 * @param number
	 */
	public void setNumber(Number number);

	/**
	 * Return the number that is stored in the graph field.
	 * 
	 * @return
	 */
	public Number getNumber();

	@Override
	default Field cloneTo(FieldContainer container) {
		NumberField clone = container.createNumber(getFieldKey());
		clone.setNumber(getNumber());
		return clone;
	}

	@Override
	default NumberFieldModel transformToRest(ActionContext ac) {
		NumberFieldModel restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return restModel;
	}

	default boolean numberEquals(Object obj) {
		if (obj instanceof NumberField) {
			Number valueA = getNumber();
			Number valueB = ((NumberField) obj).getNumber();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof NumberFieldModel) {
			Number valueA = getNumber();
			Number valueB = ((NumberFieldModel) obj).getNumber();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}
