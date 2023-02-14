package com.gentics.mesh;

import java.util.Optional;

/**
 * List of basic element types which are stored in Gentics Mesh.
 */
public enum ElementType {

	JOB,

	LANGUAGE,

	USER,

	GROUP,

	ROLE,

	SCHEMA,

	SCHEMAVERSION,

	MICROSCHEMA,

	MICROSCHEMAVERSION,

	PROJECT,

	TAGFAMILY,

	TAG,

	BRANCH,

	NODE;

	/**
	 * Parse the string value into the Mesh element type, if possible.
	 * 
	 * @param name
	 * @return
	 */
	public static final Optional<ElementType> parse(String name) {
		try {
			return Optional.ofNullable(Enum.valueOf(ElementType.class, name.toUpperCase()));
		} catch (IllegalArgumentException | NullPointerException e) {
			return Optional.empty();
		}
	}
}
