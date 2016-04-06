package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.GraphField;

@FunctionalInterface
public interface FieldFetcher {
	GraphField fetch(GraphFieldContainer container, String name);
}
