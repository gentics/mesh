package com.gentics.mesh.madl.type;

import com.gentics.mesh.madl.field.FieldMap;

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
