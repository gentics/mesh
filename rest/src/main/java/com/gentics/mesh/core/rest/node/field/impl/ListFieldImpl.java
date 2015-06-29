package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.node.field.ListField;

public class ListFieldImpl implements ListField {

	@Override
	public String getType() {
		return FieldTypes.LIST.toString();
	}
}
