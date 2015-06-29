package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.model.FieldTypes;

public class ListFieldImpl implements ListField {

	@Override
	public String getType() {
		return FieldTypes.LIST.toString();
	}
}
