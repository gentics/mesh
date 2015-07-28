package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NodeField;

public class NodeFieldImpl implements NodeField {

	private String uuid;

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public NodeField setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}
}
