package com.gentics.mesh.core.rest.tag;

/**
 * POJO for a tag reference model.
 */
public class TagReference {

	private String name;
	private String uuid;

	public TagReference() {
	}

	/**
	 * Return the name of the tag.
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

	/**
	 * Return the uuid of the tag.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the tag.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
