package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.BooleanField;

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

}
