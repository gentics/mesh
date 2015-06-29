package com.gentics.mesh.core.data.model.impl;

import com.gentics.mesh.core.data.model.TagFieldContainer;


public class TagFieldContainerImpl extends AbstractBasicFieldContainerImpl implements TagFieldContainer {

	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}
}
