package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a tag update request model.
 */
public class TagUpdateRequest implements RestModel {

	private String name;

	public TagUpdateRequest() {
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
