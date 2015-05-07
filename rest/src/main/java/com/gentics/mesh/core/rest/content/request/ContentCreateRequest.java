package com.gentics.mesh.core.rest.content.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContentCreateRequest extends ContentUpdateRequest {

	@JsonIgnore
	private String uuid;

	private String tagUuid;

	// TODO maybe we want to set the tagPath as well (alternative to tagUuid)

	public ContentCreateRequest() {
	}

	public String getTagUuid() {
		return tagUuid;
	}

	public void setTagUuid(String tagUuid) {
		this.tagUuid = tagUuid;
	}

}
