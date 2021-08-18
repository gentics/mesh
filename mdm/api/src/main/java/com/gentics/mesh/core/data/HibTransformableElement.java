package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

import io.reactivex.Single;

/**
 * A marker interface for elements that can be transformed to a rest model.
 */
public interface HibTransformableElement<T extends RestModel> {

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
		return Single.just(transformToRestSync(ac, level, languageTags));
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
	 * @deprecated Use DAO method to transform elements instead
	 */
	@Deprecated
	T transformToRestSync(InternalActionContext ac, int level, String... languageTags);

	/**
	 * Return the etag for the element.
	 * 
	 * @param ac
	 * @return Generated etag
	 */
	String getETag(InternalActionContext ac);

}
