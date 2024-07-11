package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;

@FunctionalInterface
public interface FieldFetcher {

	/**
	 * Return the {@link Field} of the given content.
	 * 
	 * @param container
	 *            Content
	 * @param name
	 *            Name of the field
	 * @return Found graph field
	 */
	Field fetch(FieldContainer container, String name);
}
