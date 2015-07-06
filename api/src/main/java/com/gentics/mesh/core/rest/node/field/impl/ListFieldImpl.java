package com.gentics.mesh.core.rest.node.field.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.ListableField;

public class ListFieldImpl<T extends ListableField> implements ListField<T> {

	private List<T> items = new ArrayList<>();

	@Override
	public List<T> getItems() {
		return items;
	}

	@Override
	public String getType() {
		return FieldTypes.LIST.toString();
	}
}
