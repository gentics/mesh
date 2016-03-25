package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

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
	 * Update the vertex using the action context information.
	 * 
	 * @param ac
	 */
	Observable<? extends V> update(InternalActionContext ac);
}
