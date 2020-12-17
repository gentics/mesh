package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.GraphField;

@FunctionalInterface
public interface FieldFetcher {

	/**
	 * Return the {@link GraphField} of the given content.
	 * 
	 * @param container
	 *            Content
	 * @param name
	 *            Name of the field
	 * @return Found graph field
	 */
	GraphField fetch(GraphFieldContainer container, String name);
}
