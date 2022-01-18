package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;

/**
 * Aggregation node for tag families.
 */
public interface TagFamilyRoot extends RootVertex<TagFamily>, TransformableElementRoot<TagFamily, TagFamilyResponse> {

	/**
	 * Return the project which is the root element of this tagfamily.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Add the tag family to the aggregation node.
	 *
	 * @param tagFamily
	 */
	void addTagFamily(TagFamily tagFamily);
}
