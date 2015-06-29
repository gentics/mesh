package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.model.FieldTypes;

public class NodeFieldImpl implements NodeField {

	private String uuid;

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}
}
