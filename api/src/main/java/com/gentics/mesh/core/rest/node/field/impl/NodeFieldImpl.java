package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NodeField;

public class NodeFieldImpl implements NodeField {

	private String uuid;

	private String path;

	@Override
	public String getUuid() {
		return uuid;
	}

	public NodeField setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@Override
	public String getPath() {
		return path;
	}

	/**
	 * Set the webroot path
	 * 
	 * @param path webroot path
	 * @return this instance
	 */
	public NodeField setPath(String path) {
		this.path = path;
		return this;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}
}
