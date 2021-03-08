package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.GraphFieldContainer;

@FunctionalInterface
public interface DataProvider {
	void set(GraphFieldContainer container, String name);
}