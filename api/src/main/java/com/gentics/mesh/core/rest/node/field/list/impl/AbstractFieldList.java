package com.gentics.mesh.core.rest.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.ListableField;

public abstract class AbstractFieldList<T extends ListableField> implements Field {

	List<T> list = new ArrayList<>();

	public List<T> getList() {
		return list;
	}

	@Override
	public String getType() {
		return "list";
	}
}
