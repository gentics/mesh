package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class TagUpdateRequest extends AbstractRestModel {

	private TagFamilyReference tagFamilyReference;

	private TagFieldContainer fields = new TagFieldContainer();

	public TagUpdateRequest() {
	}

	public TagFieldContainer getFields() {
		return fields;
	}

	public void setFields(TagFieldContainer fields) {
		this.fields = fields;
	}

	public TagFamilyReference getTagFamilyReference() {
		return tagFamilyReference;
	}

	public void setTagFamilyReference(TagFamilyReference tagFamilyReference) {
		this.tagFamilyReference = tagFamilyReference;
	}

}
