package com.gentics.cailun.core.data.model;

public enum PropertyType {

	I18N_STRING("i18n-string"), STRING("string"), NUMBER("number"), DATE("date"), BOOLEAN("boolean"), NULL("null"), HTML("html");

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

	public static PropertyType valueOfName(String name) {
		for (PropertyType type : PropertyType.values()) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		return null;

	}

}
