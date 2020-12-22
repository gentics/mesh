package com.gentics.mesh.core.rest.event.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

/**
 * REST model for tag related events.
 */
public class TagMeshEventModel extends AbstractProjectEventModel implements TagElementEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the tag family of the tag.")
	private TagFamilyReference tagFamily;

	public TagMeshEventModel() {
	}

	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

}
