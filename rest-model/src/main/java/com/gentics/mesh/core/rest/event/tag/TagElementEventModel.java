package com.gentics.mesh.core.rest.event.tag;

import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public interface TagElementEventModel extends MeshProjectElementEventModel {

	/**
	 * Return the tag family of the tag.
	 * 
	 * @return
	 */
	TagFamilyReference getTagFamily();

	/**
	 * Set the tag family reference of the tag.
	 * 
	 * @param tagFamily
	 */
	void setTagFamily(TagFamilyReference tagFamily);

}
