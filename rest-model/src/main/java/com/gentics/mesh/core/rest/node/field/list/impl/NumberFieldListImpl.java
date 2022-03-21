package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST model for a number list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class NumberFieldListImpl extends AbstractFieldList<Number> {
	@Override
	public String getItemType() {
		return FieldTypes.NUMBER.toString();
	}
}