package com.gentics.mesh.core.data.model;

public class TagFieldContainer extends AbstractFieldContainer {

	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}
}
