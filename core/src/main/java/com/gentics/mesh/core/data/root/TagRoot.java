package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Tag;

/**
 * Aggregation node for tags.
 */
public interface TagRoot extends RootVertex<Tag> {

	public static final String TYPE = "tags";

	/**
	 * Add the given tag to the aggregation vertex.
	 * 
	 * @param tag
	 *            Tag to be added
	 */
	void addTag(Tag tag);

	/**
	 * Remove the tag from the aggregation vertex.
	 * 
	 * @param tag
	 *            Tag to be removed
	 */
	void removeTag(Tag tag);

}
