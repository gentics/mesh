package com.gentics.cailun.core.rest.model;

public enum PropertyType {

	I18N_STRING("i18n-string"), STRING("string"), NUMBER("number"), DATE("date"), BOOLEAN("boolean"), NULL("null");

	private String name;

	private PropertyType(String name) {
		this.name = name;
	}

}
