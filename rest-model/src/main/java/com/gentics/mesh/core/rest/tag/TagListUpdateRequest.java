package com.gentics.mesh.core.rest.tag;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST model for a tag update request which contains a set of tag references.
 */
public class TagListUpdateRequest implements RestModel {

	@JsonPropertyDescription("List of tags which should be assigned to the node. Tags which are not included will be removed from the node.")
	@JsonProperty(required = true)
	private List<TagReference> tags = new ArrayList<>();

	/**
	 * Return a list of tag references.
	 * 
	 * @return
	 */
	public List<TagReference> getTags() {
		return tags;
	}

	/**
	 * Set the list of tag references.
	 * 
	 * @param tags
	 * @return Fluent API
	 */
	public TagListUpdateRequest setTags(List<TagReference> tags) {
		this.tags = tags;
		return this;
	}

}
