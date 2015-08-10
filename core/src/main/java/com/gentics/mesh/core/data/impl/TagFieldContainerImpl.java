package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.TagFieldContainer;


public class TagFieldContainerImpl extends AbstractBasicFieldContainerImpl implements TagFieldContainer {

	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}
	
	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}
}
