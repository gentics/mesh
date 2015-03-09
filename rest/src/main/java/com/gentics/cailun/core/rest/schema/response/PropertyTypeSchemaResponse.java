package com.gentics.cailun.core.rest.schema.response;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class PropertyTypeSchemaResponse extends AbstractRestModel {

	private String type;
	private String key;
	private String desciption;

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

}
