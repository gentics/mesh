package com.gentics.mesh.core.rest.common;

/**
 * Base class for named references. A named reference is a reference to a element within mesh that can be identified by uuid and name.
 *
 * @param <T>
 */
public abstract class NameUuidReference<T> {

	private String name;
	private String uuid;

	public NameUuidReference() {
	}

	public NameUuidReference(String name, String uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	/**
	 * Return the name of the referenced element.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the referenced element.
	 * 
	 * @param name
	 * @return
	 */
	public T setName(String name) {
		this.name = name;
		return (T) this;
	}

	/**
	 * Return the uuid of element that is referenced.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the referenced element.
	 * 
	 * @param uuid
	 * @return
	 */
	public T setUuid(String uuid) {
		this.uuid = uuid;
		return (T) this;
	}
}
