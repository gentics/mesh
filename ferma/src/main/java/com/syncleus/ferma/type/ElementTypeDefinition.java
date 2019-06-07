package com.syncleus.ferma.type;

import com.syncleus.ferma.index.field.FieldMap;

public interface ElementTypeDefinition {

	/***
	 * Return the super class of the type.
	 * 
	 * @return
	 */
	Class<?> getSuperClazz();

	/**
	 * Return the fields for the type.
	 * 
	 * @return
	 */
	FieldMap getFields();
}
