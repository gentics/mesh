package com.syncleus.ferma.index;

import com.syncleus.ferma.index.field.FieldMap;

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

}
