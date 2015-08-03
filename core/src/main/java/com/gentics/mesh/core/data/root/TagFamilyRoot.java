package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.TagFamily;

/**
 * Aggregation node for tag families.
 *
 */
public interface TagFamilyRoot extends RootVertex<TagFamily> {

	TagFamily create(String name);

	void removeTagFamily(TagFamily tagFamily);

	void addTagFamily(TagFamily tagFamily);

}
