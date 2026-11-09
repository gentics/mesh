package com.gentics.mesh.plugin.comment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.security.InvalidParameterException;

public enum Order {
	@JsonProperty("asc")
	ASCENDING("asc"),
	@JsonProperty("desc")
	DESCENDING("desc");

	private final String paramValue;

	Order(String paramValue) {
		this.paramValue = paramValue;
	}

	public static Order fromString(String value) {
		switch (value) {
			case "asc":
				return ASCENDING;
			case "desc":
				return DESCENDING;
		}
		throw new InvalidParameterException("Could not find order for value " + value);
	}

	public String getParamValue() {
		return paramValue;
	}
}
