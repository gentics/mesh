package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST model for a boolean list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class BooleanFieldListImpl extends AbstractFieldList<Boolean> {
	@Override
	public String getItemType() {
		return FieldTypes.BOOLEAN.toString();
	}
}