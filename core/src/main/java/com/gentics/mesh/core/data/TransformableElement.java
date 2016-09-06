package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.Database;

import rx.Single;

/**
 * A {@link TransformableElement} is a node that can be transformed into a rest model response.
 *
 * @param <T>
 *            RestModel response class
 */
public interface TransformableElement<T extends RestModel> extends MeshElement {

	/**
	 * Return the API path to the element.
	 * 
	 * @param ac
	 * 
	 * @return API path or null if the element has no public path
	 */
	String getAPIPath(InternalActionContext ac);

	/**
	 * Transform the element into the matching rest model response asynchronously.
	 * 
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 */
	default Single<T> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		Database db = MeshInternal.get().database();
		return db.asyncNoTx(() -> {
			return transformToRestSync(ac, level, languageTags);
		});
	}

	/**
	 * Transform the element into the matching rest model response synchronously.
	 *
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 * @return
	 */
	Single<T> transformToRestSync(InternalActionContext ac, int level, String... languageTags);

	/**
	 * Return the etag for the element.
	 * 
	 * @param ac
	 * @return
	 */
	String getETag(InternalActionContext ac);

}
