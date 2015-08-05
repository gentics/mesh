package com.gentics.mesh.graphdb.orientdb;

import com.syncleus.ferma.AbstractVertexFrame;

public class Person extends AbstractVertexFrame {

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getName() {
		return getProperty("name");
	}

}
