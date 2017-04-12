package com.gentics.mesh.core.rest.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a tag response model.
 */
public class TagResponse extends AbstractGenericRestResponse {

	@JsonPropertyDescription("Reference to the tag family to which the tag belongs.")
	@JsonProperty(required = true)
	private TagFamilyReference tagFamily;

	@JsonPropertyDescription("Name of the tag.")
	@JsonProperty(required = true)
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
	 * @return Fluent API
	 */
	public TagResponse setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
		return this;
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
	 * @return Fluent API
	 */
	public TagResponse setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		return "tag: " + getName() + "/" + getUuid() + " of family " + getTagFamily().getName();
	}

}
