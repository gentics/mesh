package com.gentics.cailun.core.data.model;

public enum PropertyType {

	I18N_STRING("i18n-string"), STRING("string"), NUMBER("number"), DATE("date"), BOOLEAN("boolean"), NULL("null");

	private String name;

	private PropertyType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
