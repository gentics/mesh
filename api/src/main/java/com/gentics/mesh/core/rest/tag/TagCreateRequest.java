package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

public class TagCreateRequest implements RestModel {

	private String name;

	public TagCreateRequest() {
	}

	/**
	 * Get the name of the tag.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the tag.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

}
