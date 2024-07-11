package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.FieldContainer;

@FunctionalInterface
public interface DataAsserter {
	void assertThat(FieldContainer container, String name);
}
