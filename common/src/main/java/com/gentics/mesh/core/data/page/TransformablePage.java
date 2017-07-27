package com.gentics.mesh.core.data.page;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;

import rx.Single;

/**
 * A transformable page is a page which contains {@link TransformableElement}. Thus it is possible to compute the etag for the page and transform the page into
 * a rest list model.
 * 
 * @param <T>
 *            Type of the page element
 */
public interface TransformablePage<T extends TransformableElement<? extends RestModel>> extends Page<T> {

	/**
	 * Transform the page into a list response.
	 * 
	 * @param ac
	 * @param level
	 *            Level of transformation
	 */
	Single<? extends ListResponse<RestModel>> transformToRest(InternalActionContext ac, int level);

	/**
	 * Return the eTag of the page. The etag is calculated using the following information:
	 * <ul>
	 * <li>Number of total elements (all pages)</li>
	 * <li>All etags for all found elements</li>
	 * <li>Number of the current page</li>
	 * </ul>
	 * 
	 * @param ac
	 * @return
	 */
	String getETag(InternalActionContext ac);
}
