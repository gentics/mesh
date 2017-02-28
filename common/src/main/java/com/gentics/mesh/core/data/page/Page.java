package com.gentics.mesh.core.data.page;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;

import rx.Single;

/**
 * A page is the result of a query which returns paged data. Each page may contain multiple items. A page can be transformed into a rest response.
 */
public interface Page<T extends TransformableElement<? extends RestModel>> extends Iterable<T> {

	/**
	 * Return the per page parameter value.
	 * 
	 * @return
	 */
	long getPerPage();

	/**
	 * Return the number of element which this page is currently providing.
	 * 
	 * @return
	 */
	long getNumberOfElements();

	/**
	 * Return the total amount of pages which the resources that provided this page could return.
	 * 
	 * @return
	 */
	long getTotalPages();

	/**
	 * Return the current page number.
	 * 
	 * @return
	 */
	long getNumber();

	/**
	 * Return the total item count which the resource that provided this page could return.
	 * 
	 * @return
	 */
	long getTotalElements();

	/**
	 * Return the page size.
	 * 
	 * @return
	 */
	int getSize();

	/**
	 * Transform the page into a list response.
	 * 
	 * @param ac
	 * @param level
	 *            Level of transformation
	 */
	Single<? extends ListResponse<RestModel>> transformToRest(InternalActionContext ac, int level);

	/**
	 * Set the paging parameters into the given list response by examining the given page.
	 * 
	 * @param response
	 *            List response that will be updated
	 */
	void setPaging(ListResponse<?> response);

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
