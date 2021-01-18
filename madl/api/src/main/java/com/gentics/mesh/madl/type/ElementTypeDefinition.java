package com.gentics.mesh.madl.type;

import com.gentics.mesh.madl.field.FieldMap;

/**
 * Type definition with a parent super class and fields.
 */
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
