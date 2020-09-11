package com.gentics.mesh.madl.index;

import com.gentics.mesh.madl.field.FieldMap;

public interface ElementIndexDefinition {

	/**
	 * Name of the index (either based on label or vertex type/class)
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Whether the index elements should be unique.
	 * 
	 * @return
	 */
	boolean isUnique();

	/**
	 * Return the fields which are part of the index.
	 * 
	 * @return
	 */
	FieldMap getFields();

	/**
	 * Return the postfix of the index.
	 * 
	 * @return
	 */
	String getPostfix();

	/**
	 * Return the index type for the index.
	 * 
	 * @return
	 */
	IndexType getType();

}
