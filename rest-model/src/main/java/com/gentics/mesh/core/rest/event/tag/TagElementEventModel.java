package com.gentics.mesh.core.rest.event.tag;

import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public interface TagElementEventModel extends MeshProjectElementEventModel {

	TagFamilyReference getTagFamily();

	void setTagFamily(TagFamilyReference tagFamily);

}
