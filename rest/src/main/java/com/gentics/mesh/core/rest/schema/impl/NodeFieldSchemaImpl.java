package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;

public class NodeFieldSchemaImpl extends AbstractFieldSchema implements NodeFieldSchema {

	@JsonProperty("allow")
	private String[] allowedSchemas;

	private String defaultUuid;

	@Override
	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public void setAllowedSchemas(String[] allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
	}

	@Override
	public String getUuid() {
		return defaultUuid;
	}

	@Override
	public void setUuid(String uuid) {
		this.defaultUuid = uuid;
	}

}
