package com.gentics.mesh.core.rest.tag.response;

import com.gentics.mesh.core.rest.common.response.AbstractPropertyContainerModel;

public class TagResponse extends AbstractPropertyContainerModel {

	private TagFamilyReference tagFamily;

	private String value;

	public TagResponse() {
	}

	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
