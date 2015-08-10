package com.gentics.mesh.core.data.root;

import java.util.Stack;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Tag;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Aggregation node for tags.
 */
public interface TagRoot extends RootVertex<Tag> {

	public static final String TYPE = "tags";
	
	/**
	 * Add the given tag to the aggregation vertex.
	 * 
	 * @param tag
	 */
	void addTag(Tag tag);

	/**
	 * Remove the tag from the aggregation vertex.
	 * 
	 * @param tag
	 */
	void removeTag(Tag tag);

}
