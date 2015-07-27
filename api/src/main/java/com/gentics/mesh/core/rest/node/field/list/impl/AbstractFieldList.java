package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.node.field.Field;

public abstract class AbstractFieldList implements Field {

	@Override
	public String getType() {
		return "list";
	}
}
