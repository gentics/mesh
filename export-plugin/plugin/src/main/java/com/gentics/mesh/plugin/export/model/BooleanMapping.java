package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BooleanMapping {

	@JsonProperty(value = "true")
	private String trueValue;

	@JsonProperty(value = "false")
	private String falseValue;

	@JsonProperty(value = "null")
	private String nullValue;

	public BooleanMapping() {
	}

	public String getTrueValue() {
		return trueValue;
	}

	public BooleanMapping setTrueValue(String trueValue) {
		this.trueValue = trueValue;
		return this;
	}

	public String getFalseValue() {
		return falseValue;
	}

	public BooleanMapping setFalseValue(String falseValue) {
		this.falseValue = falseValue;
		return this;
	}

	public String getNullValue() {
		return nullValue;
	}

	public BooleanMapping setNullValue(String nullValue) {
		this.nullValue = nullValue;
		return this;
	}

	public String getStringValue(Boolean bool) {
		if (bool == null) {
			return nullValue;
		}
		return bool ? trueValue : falseValue;
	}
}
