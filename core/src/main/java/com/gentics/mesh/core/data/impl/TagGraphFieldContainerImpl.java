package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.TagGraphFieldContainer;

public class TagGraphFieldContainerImpl extends AbstractBasicGraphFieldContainerImpl implements TagGraphFieldContainer {

	public String getName() {
		return getProperty("name");
	}

	public TagGraphFieldContainer setName(String name) {
		setProperty("name", name);
		return this;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}
}
