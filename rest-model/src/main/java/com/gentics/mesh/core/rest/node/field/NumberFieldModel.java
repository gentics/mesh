package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST POJO for number field information. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public interface NumberFieldModel extends ListableFieldModel, MicroschemaListableFieldModel {

	/**
	 * Return the number value.
	 * 
	 * @return Number value
	 */
	Number getNumber();

	/**
	 * Set the number field value.
	 * 
	 * @param number
	 *            Number value
	 * @return Fluent API
	 */
	NumberFieldModel setNumber(Number number);

	@Override
	default Object getValue() {
		return getNumber();
	}
}
