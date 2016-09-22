package com.gentics.mesh.core.rest.common;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

	/**
	 * Create a new reference that provides the name and uuid.
	 * 
	 * @param name
	 * @param uuid
	 */
	public NameUuidReference(String name, String uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	/**
	 * Return the name of the referenced element.
	 * 
	 * @return Name of the referenced element
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the referenced element.
	 * 
	 * @param name
	 *            Name of the referenced element
	 * @return Fluent API
	 */
	@SuppressWarnings("unchecked")
	public T setName(String name) {
		this.name = name;
		return (T) this;
	}

	/**
	 * Return the uuid of element that is referenced.
	 * 
	 * @return Uuid of the referenced element
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the referenced element.
	 * 
	 * @param uuid
	 *            Uuid of the referenced element
	 * @return Fluent API
	 */
	@SuppressWarnings("unchecked")
	public T setUuid(String uuid) {
		this.uuid = uuid;
		return (T) this;
	}

	/**
	 * Checks whether one of the needed parameters (name or uuid) is set.
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isSet() {
		return !StringUtils.isEmpty(getName()) || !StringUtils.isEmpty(getUuid());
	}
}
