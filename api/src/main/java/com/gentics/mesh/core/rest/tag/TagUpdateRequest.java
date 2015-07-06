package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

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
