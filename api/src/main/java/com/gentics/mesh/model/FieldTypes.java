package com.gentics.mesh.model;

public enum FieldTypes {
	STRING, HTML, NUMBER, DATE, BOOLEAN, SELECT, NODE, LIST, MICROSCHEMA;

	public String toString() {
		return name().toLowerCase();
	};

}
