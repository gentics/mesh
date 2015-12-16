package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NodeField;

public class NodeFieldImpl implements NodeField {

	private String uuid;

	private String url;

	@Override
	public String getUuid() {
		return uuid;
	}

	public NodeField setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@Override
	public String getUrl() {
		return url;
	}

	/**
	 * Set the webroot URL
	 * 
	 * @param url webroot URL
	 * @return this instance
	 */
	public NodeField setUrl(String url) {
		this.url = url;
		return this;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}
}
