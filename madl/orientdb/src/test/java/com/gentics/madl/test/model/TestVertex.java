package com.gentics.madl.test.model;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.frame.AbstractVertexFrame;

@GraphElement
public class TestVertex extends AbstractVertexFrame {

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getName() {
		return getProperty("name");
	}

}
