package com.gentics.mesh.core.data.model.schema.propertytype;

public enum PropertyType {

	I18N_STRING("i18n-string"), STRING("string"), NUMBER("number"), DATE("date"), BOOLEAN("boolean"), HTML("html"), REFERENCE(
			"reference"), LIST("list"), MICROSCHEMA("microschema");

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
