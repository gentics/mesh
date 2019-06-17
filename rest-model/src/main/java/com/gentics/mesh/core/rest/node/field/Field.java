package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.RestModel;

public interface Field extends RestModel {

	/**
	 * Return the field type.
	 * 
	 * @return Type of the field
	 */
	public String getType();

	/**
	 * Return the value stored in the field.
	 * @return
	 */
	@JsonIgnore
	Object getValue();
}
