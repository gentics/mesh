package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.HibFieldContainer;

@FunctionalInterface
public interface DataProvider {
	void set(HibFieldContainer container, String name);
}