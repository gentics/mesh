package com.gentics.mesh.core.rest.tag;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a tag response model.
 */
public class TagResponse extends AbstractGenericRestResponse {

	@JsonPropertyDescription("Reference to the tag family to which the tag belongs.")
	private TagFamilyReference tagFamily;

	@JsonPropertyDescription("Name of the tag.")
	private String name;

	public TagResponse() {
	}

	/**
	 * Return the tag family reference.
	 * 
	 * @return Tag family reference
	 */
	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	/**
	 * Set the tag family reference.
	 * 
	 * @param tagFamily
	 */
	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
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
