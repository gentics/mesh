package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.FieldContainer;

@FunctionalInterface
public interface DataProvider {
	void set(FieldContainer container, String name);
}