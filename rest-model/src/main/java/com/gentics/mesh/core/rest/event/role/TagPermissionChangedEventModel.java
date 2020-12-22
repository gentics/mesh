package com.gentics.mesh.core.rest.event.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.tag.TagElementEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

/**
 * POJO for tag permission change events.
 */
public class TagPermissionChangedEventModel extends PermissionChangedProjectElementEventModel implements TagElementEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the tagfamily of the tag.")
	private TagFamilyReference tagFamily;

	@Override
	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	@Override
	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

}
