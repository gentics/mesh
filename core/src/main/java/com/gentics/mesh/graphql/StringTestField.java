package com.gentics.mesh.graphql;

public class StringTestField implements TestField {

	String name;

	boolean encoded;

	public StringTestField(String name, boolean isEncoded) {
		this.name = name;
		this.encoded = isEncoded;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEncoded() {
		return encoded;
	}

	public void setEncoded(boolean encoded) {
		this.encoded = encoded;
	}

}
