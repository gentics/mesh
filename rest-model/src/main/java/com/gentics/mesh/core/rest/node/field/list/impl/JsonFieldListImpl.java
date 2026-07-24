package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.JsonContent;

/**
 * REST model for a JSON object list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class JsonFieldListImpl extends AbstractFieldList<JsonContent> {

	@Override
	public String getItemType() {
		return FieldTypes.JSON.toString();
	}
}
