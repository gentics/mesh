package com.gentics.mesh.core.field;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;

@FunctionalInterface
public interface FieldFetcher {

	/**
	 * Return the {@link HibField} of the given content.
	 * 
	 * @param container
	 *            Content
	 * @param name
	 *            Name of the field
	 * @return Found graph field
	 */
	HibField fetch(HibFieldContainer container, String name);
}
