package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.HibFieldContainer;

@FunctionalInterface
public interface DataAsserter {
	void assertThat(HibFieldContainer container, String name);
}
