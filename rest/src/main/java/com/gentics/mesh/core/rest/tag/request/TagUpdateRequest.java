package com.gentics.mesh.core.rest.tag.request;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.tag.response.TagFamilyReference;

public class TagUpdateRequest extends AbstractRestModel {

	private TagFamilyReference tagFamilyReference;

	private String name;

	public TagUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TagFamilyReference getTagFamilyReference() {
		return tagFamilyReference;
	}

	public void setTagFamilyReference(TagFamilyReference tagFamilyReference) {
		this.tagFamilyReference = tagFamilyReference;
	}

}
