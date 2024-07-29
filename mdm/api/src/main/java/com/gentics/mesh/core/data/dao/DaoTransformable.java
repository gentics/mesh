package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.common.RestModel;

import io.reactivex.Single;

/**
 * DAO for POJO-DTO transformation functionality.
 * 
 * @param <T>
 *            Domain class type
 * @param <R>
 *            Rest model type
 */
public interface DaoTransformable<T, R extends RestModel> {
	/**
	 * Invoked before transforming the elements in the page to their REST models
	 * @param page page of elements
	 * @param ac action context
	 */
	default void beforeTransformToRestSync(Page<? extends HibCoreElement<? extends RestModel>> page, InternalActionContext ac) {}

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
	 * Invoked before retrieving etags for a page
	 * @param page
	 * @param ac
	 */
	default void beforeGetETagForPage(Page<? extends HibCoreElement<? extends RestModel>> page, InternalActionContext ac) {}

	/**
	 * Return the etag for the element.
	 *
	 * @param element
	 * @param ac
	 * @return Generated etag
	 */
	default String getETag(HibTransformableElement<? extends RestModel> element, InternalActionContext ac) {
		return element.getETag(ac);
	}

	/**
	 * Return the API path.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	default String getAPIPath(HibTransformableElement<? extends RestModel> element, InternalActionContext ac) {
		return element.getAPIPath(ac);
	}
}
