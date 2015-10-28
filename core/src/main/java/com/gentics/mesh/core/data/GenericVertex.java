package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface GenericVertex<T extends RestModel> extends MeshVertex, TransformableNode<T> {

	/**
	 * Return the type of the vertex.
	 * 
	 * @return
	 */
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

	/**
	 * Update the vertex using the action context information and invoke the handler once the update has been completed.
	 * 
	 * @param ac
	 * @param handler
	 */
	void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler);

	/**
	 * Set the editor and creator references and update the timestamps for created and edited fields.
	 * 
	 * @param user
	 */
	void setCreated(User user);

}
