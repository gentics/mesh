package com.syncleus.ferma.index;

import com.syncleus.ferma.index.field.FieldMap;

public interface ElementIndexDefinition {

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

}
