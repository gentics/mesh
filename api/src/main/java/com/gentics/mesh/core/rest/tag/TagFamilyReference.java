package com.gentics.mesh.core.rest.tag;

/**
 * A tag family reference is a basic rest model POJO that contains a reference to a tag family in form of the tag family uuid and the tag family name.
 */
public class TagFamilyReference {

	private String name;
	private String uuid;

	public TagFamilyReference() {
	}

	/**
	 * Name of the tag family.
	 * 
	 * @return Name of the tag family
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the tag family.
	 * 
	 * @param name
	 *            Name of the tag family
	 * @return Fluent API
	 */
	public TagFamilyReference setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the name of the uuid.
	 * 
	 * @return Tag family uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the name of the tagfamily uuid.
	 * 
	 * @param uuid
	 *            Tag family uuid
	 * @return Fluent API
	 */
	public TagFamilyReference setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

}
