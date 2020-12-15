package com.gentics.mesh.core.rest.node.field;

/**
 * REST definition of list types.
 */
public enum ListTypes {

	STRING_LIST("string"),

	HTML_LIST("html"),

	NUMBER_LIST("number"),

	DATE_LIST("date"),

	BOOLEAN_LIST("boolean"),

	MICRONODE_LIST("micronode"),

	NODE_LIST("node");

	private String type;

	private ListTypes(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public static ListTypes valueByName(String listType) {
		for (ListTypes type : values()) {
			if (type.toString().equals(listType)) {
				return type;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return type;
	}
}