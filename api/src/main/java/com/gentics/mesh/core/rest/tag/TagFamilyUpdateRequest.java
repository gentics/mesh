package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a tag family update model.
 */
public class TagFamilyUpdateRequest implements RestModel {

	private String name;

	/**
	 * Set the name of the tag family.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the tag family.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

}
