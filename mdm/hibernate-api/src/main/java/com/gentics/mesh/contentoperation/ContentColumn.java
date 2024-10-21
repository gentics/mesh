package com.gentics.mesh.contentoperation;


/**
 * A column of a content table
 */
public interface ContentColumn {

	/**
	 *
	 * @return the name of the column
	 */
	String getLabel();

	/**
	 * @return the java class that will be used to deserialize the result set
	 */
	Class<?> getJavaClass();

	/**
	 * Transform the given value to its persisted form
	 * @param value value to be stored
	 * @return persisted form
	 */
	default Object transformToPersistedValue(Object value) {
		return value;
	}
}
