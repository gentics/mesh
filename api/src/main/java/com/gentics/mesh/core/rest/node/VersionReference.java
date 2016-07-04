package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.AbstractResponse;

/**
 * REST Model for the Version Reference of a NodeResponse
 */
public class VersionReference extends AbstractResponse {
	private String number;

	/**
	 * Empty Constructor
	 */
	public VersionReference() {
	}

	/**
	 * Create an instance with uuid and number
	 * 
	 * @param uuid
	 *            field container uuid
	 * @param number
	 *            version number
	 */
	public VersionReference(String uuid, String number) {
		this();
		setUuid(uuid);
		setNumber(number);
	}

	/**
	 * Get the version number
	 * 
	 * @return version number
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * Set the version number
	 * 
	 * @param number
	 *            version number
	 * @return Fluent API
	 */
	public VersionReference setNumber(String number) {
		this.number = number;
		return this;
	}

	@Override
	public String toString() {
		return getNumber();
	}
}
