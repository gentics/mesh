package com.gentics.mesh.core.rest.release;

import com.gentics.mesh.core.rest.common.RestModel;

public class ReleaseCreateRequest implements RestModel {

	private String name;

	public ReleaseCreateRequest() {
	}

	/**
	 * Return the release name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the release name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public ReleaseCreateRequest setName(String name) {
		this.name = name;
		return this;
	}
}
