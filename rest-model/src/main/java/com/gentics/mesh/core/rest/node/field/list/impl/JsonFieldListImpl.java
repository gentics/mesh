package com.gentics.mesh.core.rest.node.field.list.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST model for a JSON object list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class JsonFieldListImpl extends AbstractFieldList<JsonNode> {

	@Override
	public String getItemType() {
		return FieldTypes.JSON.toString();
	}
}
