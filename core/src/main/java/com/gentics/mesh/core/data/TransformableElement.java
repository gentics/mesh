package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * A {@link TransformableElement} is a node that can be transformed into a rest model response.
 *
 * @param <T>
 *            RestModel response class
 */
public interface TransformableElement<T extends RestModel> extends MeshElement {

	/**
	 * Transform the node into the matching rest model response.
	 * 
	 * @param ac
	 *            Context of the calling action
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 */
	Observable<T> transformToRest(InternalActionContext ac, String... languageTags);

}
