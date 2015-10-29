package com.gentics.mesh.core.data;

/**
 * Graph domain model element which is used to store tag fields. Currently only the tag name is stored within this field.
 */
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
