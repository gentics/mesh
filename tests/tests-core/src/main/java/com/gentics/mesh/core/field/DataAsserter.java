package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.GraphFieldContainer;

@FunctionalInterface
public interface DataAsserter {
	void assertThat(GraphFieldContainer container, String name);
}
