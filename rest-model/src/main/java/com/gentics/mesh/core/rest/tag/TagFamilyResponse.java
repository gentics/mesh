package com.gentics.mesh.core.rest.tag;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a tag family response model.
 */
public class TagFamilyResponse extends AbstractGenericRestResponse {

	@JsonPropertyDescription("Name of the tag family.")
	private String name;

	/**
	 * Return the tag family name.
	 * 
	 * @return Name of the tag family
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the tag family name.
	 * 
	 * @param name
	 *            Name of the tag family
	 * @return Fluent API
	 */
	public TagFamilyResponse setName(String name) {
		this.name = name;
		return this;
	}

}
