package com.gentics.mesh.core.rest.common;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Objects;

/**
 * Base class for named references. A named reference is a reference to a element within mesh that can be identified by uuid and name.
 *
 * @param <T>
 *            Specific type which is referenced. Used for fluent API
 */
public abstract class AbstractNameUuidReference<T> implements NameUuidReference<T> {

	@JsonPropertyDescription("Name of the referenced element")
	private String name;

	@JsonPropertyDescription("Uuid of the referenced element")
	@JsonProperty(required = true)
	private String uuid;

	public AbstractNameUuidReference() {
	}

	/**
	 * Create a new reference that provides the name and UUID.
	 * 
	 * @param name
	 * @param uuid
	 */
	public AbstractNameUuidReference(String name, String uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T setName(String name) {
		this.name = name;
		return (T) this;
	}


	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T setUuid(String uuid) {
		this.uuid = uuid;
		return (T) this;
	}

	@Override
	@JsonIgnore
	public boolean isSet() {
		return !StringUtils.isEmpty(getName()) || !StringUtils.isEmpty(getUuid());
	}

	@Override
	public String toString() {
		return "Reference: " + uuid + "/" + name + "/" + getClass().getSimpleName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof AbstractNameUuidReference) {
			AbstractNameUuidReference<?> that = (AbstractNameUuidReference<?>) o;
			return Objects.equals(getUuid(), that.getUuid());
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUuid());
	}
}
