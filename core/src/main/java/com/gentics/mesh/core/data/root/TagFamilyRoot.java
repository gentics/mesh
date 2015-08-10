package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for tag families.
 *
 */
public interface TagFamilyRoot extends RootVertex<TagFamily> {
	
	public static final String TYPE = "tagFamilies";

	TagFamily create(String name, User user);

	void removeTagFamily(TagFamily tagFamily);

	void addTagFamily(TagFamily tagFamily);

}
