package com.gentics.mesh.core.field.node;

import com.gentics.mesh.core.field.FieldFetcher;

public interface NodeFieldTestHelper {
	
	static final FieldFetcher FETCH = (container, name) -> container.getNode(name);

}
