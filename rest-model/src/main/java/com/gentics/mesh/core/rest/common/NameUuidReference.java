package com.gentics.mesh.core.rest.common;

/**
 * Basic interface for name/uuid reference entities.
 * 
 * @param <T>
 */
public interface NameUuidReference<T> {

	/**
	 * Return the name of the referenced element.
	 * 
	 * @return Name of the referenced element
	 */
	String getName();

	/**
	 * Set the name of the referenced element.
	 * 
	 * @param name
	 *            Name of the referenced element
	 * @return Fluent API
	 */
	T setName(String name);

	/**
	 * Return the UUID of element that is referenced.
	 * 
	 * @return UUID of the referenced element
	 */
	String getUuid();

	/**
	 * Set the UUID of the referenced element.
	 * 
	 * @param uuid
	 *            UUID of the referenced element
	 * @return Fluent API
	 */
	T setUuid(String uuid);

	/**
	 * Checks whether one of the needed parameters (name or UUID) is set.
	 * 
	 * @return
	 */
	boolean isSet();

}
