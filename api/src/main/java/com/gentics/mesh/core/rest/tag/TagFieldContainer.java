package com.gentics.mesh.core.rest.tag;

/**
 * POJO for a tag field container. A tag field container holds all field of a tag. The name of a tag is stored within the container.
 *
 */
public class TagFieldContainer {

	private String name;

	/**
	 * Return the name of a tag field container.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of a tag field container.
	 * 
	 * @param name
	 * @return
	 */
	public TagFieldContainer setName(String name) {
		this.name = name;
		return this;
	}

}
