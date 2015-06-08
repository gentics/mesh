package com.gentics.mesh.core.data.model.tinkerpop;

import com.tinkerpop.frames.Property;

public interface PropertyType {

	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

}
