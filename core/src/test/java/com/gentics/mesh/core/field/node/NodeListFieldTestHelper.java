package com.gentics.mesh.core.field.node;

import com.gentics.mesh.core.field.FieldFetcher;

public interface NodeListFieldTestHelper {

	static final FieldFetcher FETCH = (container, name) -> container.getNodeList(name);

}
