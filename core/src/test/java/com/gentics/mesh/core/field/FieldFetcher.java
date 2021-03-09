package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
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
	HibField fetch(HibFieldContainer container, String name);
}
