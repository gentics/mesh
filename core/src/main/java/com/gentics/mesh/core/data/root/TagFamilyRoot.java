package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for tag families.
 *
 */
public interface TagFamilyRoot extends RootVertex<TagFamily> {

	public static final String TYPE = "tagFamilies";

	/**
	 * Create a new tag family with the given name and assign creator and editor field using the provided user.
	 * 
	 * @param name
	 * @param user
	 * @return
	 */
	TagFamily create(String name, User user);

	/**
	 * Remove the tag family from the aggregation node.
	 * 
	 * @param tagFamily
	 */
	void removeTagFamily(TagFamily tagFamily);

	/**
	 * Add the tag family to the aggregation node.
	 * 
	 * @param tagFamily
	 */
	void addTagFamily(TagFamily tagFamily);

}
