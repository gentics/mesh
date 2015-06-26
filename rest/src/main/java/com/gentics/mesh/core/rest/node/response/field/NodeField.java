package com.gentics.mesh.core.rest.node.response.field;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.mesh.model.FieldTypes;

public class NodeField extends AbstractField {

	private String uuid;

	@JsonProperty("allow")
	private String[] allowedSchemas;

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	public void setAllowedSchemas(String[] allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
	}

	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}
}
