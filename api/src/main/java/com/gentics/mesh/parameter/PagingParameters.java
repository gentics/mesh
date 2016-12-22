package com.gentics.mesh.parameter;

import com.gentics.mesh.api.common.SortOrder;

public interface PagingParameters extends ParameterProvider {

	/**
	 * Return the current page.
	 * 
	 * @return Current page number
	 */
	int getPage();

	/**
	 * Return the per page count.
	 * 
	 * @return Per page count
	 */
	int getPerPage();

	/**
	 * Set the current page.
	 * 
	 * @param page
	 *            Current page number
	 * @return Fluent API
	 */
	PagingParameters setPage(int page);

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 *            Per page count
	 * @return Fluent API
	 */
	PagingParameters setPerPage(int perPage);

	/**
	 * Return the sort by parameter value.
	 * 
	 * @return Field to be sorted by
	 * @deprecated not yet implemented
	 */
	@Deprecated
	String getSortBy();

	SortOrder getOrder();

	/**
	 * Set the order by parameter.
	 * 
	 * @param orderBy
	 * @return
	 * @deprecated not yet implemented
	 */
	@Deprecated
	PagingParameters setOrderBy(String orderBy);

	/**
	 * Set the used sort order.
	 * 
	 * @param sortBy
	 *            Sort order
	 * @return Fluent API
	 * @deprecated not yet implemented
	 * 
	 */
	@Deprecated
	PagingParameters setSortOrder(String sortBy);

}
