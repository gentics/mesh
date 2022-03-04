package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST model for a date list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class DateFieldListImpl extends AbstractFieldList<String> {
	@Override
	public String getItemType() {
		return FieldTypes.DATE.toString();
	}
}
