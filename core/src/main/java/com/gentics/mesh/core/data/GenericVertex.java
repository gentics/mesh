package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;

import io.vertx.ext.web.RoutingContext;

public interface GenericVertex<T extends RestModel> extends MeshVertex, TransformableNode<T> {

	String getType();

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return
	 */
	User getCreator();

	/**
	 * Set the creator of the vertex.
	 * 
	 * @param user
	 */
	void setCreator(User user);

	/**
	 * Return the editor of the vertex.
	 * 
	 * @return
	 */
	User getEditor();

	/**
	 * Set the editor of the vertex.
	 * 
	 * @param user
	 */
	void setEditor(User user);

	/**
	 * Return the timestamp on which the vertex was last updated.
	 * 
	 * @return
	 */
	Long getLastEditedTimestamp();

	/**
	 * Set the timestamp on which the vertex was last updated.
	 * 
	 * @param timestamp
	 */
	void setLastEditedTimestamp(long timestamp);

	/**
	 * Return the timestamp on which the vertex was created.
	 * 
	 * @return
	 */
	Long getCreationTimestamp();

	/**
	 * Set the timestamp on which the vertex was created.
	 * 
	 * @param timestamp
	 */
	void setCreationTimestamp(long timestamp);

	void update(RoutingContext rc);

}
