package com.gentics.cailun.core.rest.response;

public class RestPropertyTypeSchema extends AbstractRestModel {

	private String type;
	private String key;
	private String desciption;

	public RestPropertyTypeSchema() {
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

	public void setDesciption(String desciption) {
		this.desciption = desciption;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
