package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

/**
 * POJO for a tag family response model.
 *
 */
public class TagFamilyResponse extends AbstractGenericNodeRestModel {

	private String name;

	/**
	 * Set the tag family name.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the tag family name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

}
