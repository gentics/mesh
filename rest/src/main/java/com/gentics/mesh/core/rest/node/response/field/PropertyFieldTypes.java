package com.gentics.mesh.core.rest.node.response.field;

public enum PropertyFieldTypes {
	STRING, HTML, NUMBER, DATE, BOOLEAN, SELECT, NODE, LIST, MICROSCHEMA;

	public String toString() {
		return name().toLowerCase();
	};

}
