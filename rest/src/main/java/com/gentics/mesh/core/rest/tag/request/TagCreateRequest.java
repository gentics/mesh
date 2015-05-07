package com.gentics.mesh.core.rest.tag.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TagCreateRequest extends TagUpdateRequest {

	@JsonIgnore
	private String uuid;

	/**
	 * Uuid of the parent tag
	 */
	private String tagUuid;

	public TagCreateRequest() {
	}

	public String getTagUuid() {
		return tagUuid;
	}

	public void setTagUuid(String tagUuid) {
		this.tagUuid = tagUuid;
	}
}
