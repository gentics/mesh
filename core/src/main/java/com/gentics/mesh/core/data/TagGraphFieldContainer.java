package com.gentics.mesh.core.data;

public interface TagGraphFieldContainer extends BasicFieldContainer {

	/**
	 * Return the tag name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the tag name
	 * 
	 * @param name
	 * @return Fluent API
	 */
	TagGraphFieldContainer setName(String name);

}
