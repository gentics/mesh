package com.gentics.mesh.core.rest.event.tag;

import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public class DeletedTagMeshEventModel extends AbstractProjectEventModel {

	private TagFamilyReference tagFamily;

	public DeletedTagMeshEventModel() {
	}

	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

}
