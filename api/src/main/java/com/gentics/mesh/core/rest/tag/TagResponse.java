package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

public class TagResponse extends AbstractGenericNodeRestModel {

	private TagFamilyReference tagFamily;

	private TagFieldContainer fields = new TagFieldContainer();

	public TagResponse() {
	}

	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	public void setTagFamilyReference(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

	public TagFamilyReference getTagFamilyReference() {
		return tagFamily;
	}

	public TagFieldContainer getFields() {
		return fields;
	}

}
