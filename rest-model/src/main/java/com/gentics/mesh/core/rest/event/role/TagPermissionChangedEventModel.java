package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.core.rest.event.tag.TagElementEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public class TagPermissionChangedEventModel extends PermissionChangedProjectElementEventModel implements TagElementEventModel {

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
