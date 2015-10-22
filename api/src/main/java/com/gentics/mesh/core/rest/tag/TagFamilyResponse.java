package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

/**
 * POJO for a tag family response model.
 */
public class TagFamilyResponse extends AbstractGenericNodeRestModel {

	private String name;

	/**
	 * Set the tag family name.
	 * 
	 * @param name
	 *            Name of the tag family
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the tag family name.
	 * 
	 * @return Name of the tag family
	 */
	public String getName() {
		return name;
	}

}
