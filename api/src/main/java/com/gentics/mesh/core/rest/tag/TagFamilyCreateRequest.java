package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a tag create request model.
 */
public class TagFamilyCreateRequest implements RestModel {

	private String name;

	public TagFamilyCreateRequest() {
	}

	/**
	 * Set the name of the tag.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the tag.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
}
