package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

import rx.Observable;

/**
 * A mesh core vertex is an vertex which can be manipulated via CRUD by the user. Thus this interface provides various methods that are needed to interact with
 * such vertices.
 * 
 * @param <R>
 *            Rest model class of the core vertex
 * @param <V>
 *            The core vertex itself
 */
public interface MeshCoreVertex<R extends RestModel, V extends MeshCoreVertex<R, V>> extends MeshVertex, IndexableElement, TransformableElement<R> {

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return Creator
	 */
	User getCreator();

	/**
	 * Set the creator of the vertex.
	 * 
	 * @param user
	 *            Creator
	 */
	void setCreator(User user);

	/**
	 * Return the editor of the vertex.
	 * 
	 * @return Editor
	 */
	User getEditor();

	/**
	 * Set the editor of the vertex.
	 * 
	 * @param user
	 *            Editor
	 */
	void setEditor(User user);

	/**
	 * Return the timestamp on which the vertex was last updated.
	 * 
	 * @return Edit timestamp
	 */
	Long getLastEditedTimestamp();

	/**
	 * Set the timestamp on which the vertex was last updated.
	 * 
	 * @param timestamp
	 *            Edit timestamp
	 */
	void setLastEditedTimestamp(long timestamp);

	/**
	 * Return the timestamp on which the vertex was created.
	 * 
	 * @return Creation timestamp
	 */
	Long getCreationTimestamp();

	/**
	 * Set the timestamp on which the vertex was created.
	 * 
	 * @param timestamp
	 *            Creation timestamp
	 */
	void setCreationTimestamp(long timestamp);

	/**
	 * Update the vertex using the action context information.
	 * 
	 * @param ac
	 */
	Observable<? extends V> update(InternalActionContext ac);

	/**
	 * Set the editor and creator references and update the timestamps for created and edited fields.
	 * 
	 * @param user
	 *            Creator
	 */
	void setCreated(User user);

}
