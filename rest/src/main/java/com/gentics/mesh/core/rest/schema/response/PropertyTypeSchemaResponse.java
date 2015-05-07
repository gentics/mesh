package com.gentics.mesh.core.rest.schema.response;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;

public class PropertyTypeSchemaResponse extends AbstractRestModel {

	private String type;
	private String key;
	private String displayName;
	private String desciption;
	private int order;

	public PropertyTypeSchemaResponse() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getDesciption() {
		return desciption;
	}

	public void setDescription(String desciption) {
		this.desciption = desciption;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
