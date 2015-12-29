package com.gentics.mesh.core.rest.microschema.impl;

import com.gentics.mesh.core.rest.schema.Microschema;

public class MicroschemaImpl implements Microschema {

	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

}
