package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A mesh core vertex is an vertex which can be manipulated via CRUD by the user. Thus this interface provides various methods that are needed to interact with
 * such vertices.
 * 
 * @param <R>
 *            Rest model class of the core vertex
 */
public interface MeshCoreVertex<R extends RestModel> extends MeshVertex, TransformableElement<R>, HibCoreElement<R> {

	/**
	 * Update the vertex using the action context information.
	 * 
	 * @param ac
	 * @param batch
	 *            Batch to which entries will be added in order to update the search index.
	 * @return true if the element was updated. Otherwise false
	 */
	boolean update(InternalActionContext ac, EventQueueBatch batch);

	@Override
	default Object getId() {
		return getElement().getId();
	}
}
