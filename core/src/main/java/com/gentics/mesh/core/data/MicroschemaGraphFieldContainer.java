package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;

public interface MicroschemaGraphFieldContainer {

	/**
	 * Create a new microschema graph field and add it to the container.
	 * 
	 * @param key
	 * @return
	 */
	MicroschemaGraphField createMicroschema(String key);

	/**
	 * Return the microschema graph field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	MicroschemaGraphField getMicroschema(String key);

}
