package com.gentics.mesh.core.data.root;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

import io.reactivex.Single;

/**
 * A {@link TransformableElementRoot} is a node that can be transformed into a rest model response.
 *
 * @param <T>
 *            RestModel response class
 */
public interface TransformableElementRoot<T, R extends RestModel> {

	/**
	 * Return the API path to the element.
	 *
	 * @param element
	 * @param ac
	 * 
	 * @return API path or null if the element has no public path
	 */
	String getAPIPath(T element, InternalActionContext ac);

	/**
	 * Transform the element into the matching rest model response asynchronously.
	 *
	 * @param element
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 */
	default Single<R> transformToRest(T element, InternalActionContext ac, int level, String... languageTags) {
		return Single.just(transformToRestSync(element, ac, level, languageTags));
	}

	/**
	 * Transform the element into the matching rest model response synchronously.
	 *
	 * @param element
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 * @return
	 */
	R transformToRestSync(T element, InternalActionContext ac, int level, String... languageTags);

	/**
	 * Return the etag for the element.
	 *
	 * @param element
	 * @param ac
	 * @return Generated etag
	 */
	String getETag(T element, InternalActionContext ac);

}
