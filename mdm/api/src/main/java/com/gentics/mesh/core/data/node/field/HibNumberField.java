package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

public interface HibNumberField extends HibListableField, HibBasicField<NumberField> {

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
	default HibField cloneTo(HibFieldContainer container) {
		HibNumberField clone = container.createNumber(getFieldKey());
		clone.setNumber(getNumber());
		return clone;
	}

	@Override
	default NumberField transformToRest(ActionContext ac) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return restModel;
	}

	default boolean numberEquals(Object obj) {
		if (obj instanceof HibNumberField) {
			Number valueA = getNumber();
			Number valueB = ((HibNumberField) obj).getNumber();
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
