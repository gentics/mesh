package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST definition of a node field. Every field has a type and a value which is field specific.
 * Please note that the {@link FieldMap} will be used to serialize / deserialize fields.
 */
public interface Field extends RestModel {

	/**
	 * Return the field type.
	 * 
	 * @return Type of the field
	 */
	public String getType();

	/**
	 * Return the value stored in the field.
	 * 
	 * @return
	 */
	@JsonIgnore
	Object getValue();
}
