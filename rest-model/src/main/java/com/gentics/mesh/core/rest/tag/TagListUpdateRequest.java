package com.gentics.mesh.core.rest.tag;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class TagListUpdateRequest implements RestModel {

	@JsonPropertyDescription("List of tags which should be assigned to the node. Tags which are not included will be removed from the node.")
	@JsonProperty(required = true)
	private List<TagReference> tags = new ArrayList<>();

	public List<TagReference> getTags() {
		return tags;
	}

	public void setTags(List<TagReference> tags) {
		this.tags = tags;
	}

}
