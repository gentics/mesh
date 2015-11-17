package com.gentics.mesh.query.impl;

import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.query.QueryParameterProvider;

/**
 * A {@link PagingParameter} can be used to add additional paging parameters to the rest requests.
 */
public class PagingParameter implements QueryParameterProvider {

	public static final String PAGE_PARAMETER_KEY = "page";
	public static final String PER_PAGE_PARAMETER_KEY = "perPage";

	private int page;
	private int perPage;
	private String sortBy;
	private SortOrder order;

	public PagingParameter(int page, int perPage, String sortBy, SortOrder order) {
		this.page = page;
		this.perPage = perPage;
		this.sortBy = sortBy;
		this.order = order;
	}

	/**
	 * Create a new paging info for page one.
	 */
	public PagingParameter() {
		this(1);
	}

	/**
	 * Create a new paging info for the given page.
	 * 
	 * @param page
	 *            Page number
	 */
	public PagingParameter(int page) {
		//TODO use reference for default page size
		this(page, 25);
	}

	/**
	 * Create a new paging info with the given values.
	 * 
	 * @param page
	 *            Page number
	 * @param perPage
	 *            Per page count
	 */
	public PagingParameter(int page, int perPage) {
		this(page, perPage, "uuid", SortOrder.ASCENDING);
	}

	/**
	 * Return the current page.
	 * 
	 * @return Current page number
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Return the per page count.
	 * 
	 * @return Per page count
	 */
	public int getPerPage() {
		return perPage;
	}

	/**
	 * Set the current page.
	 * 
	 * @param page
	 *            Current page number
	 * @return Fluent API
	 */
	public PagingParameter setPage(int page) {
		this.page = page;
		return this;
	}

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 *            Per page count
	 * @return Fluent API
	 */
	public PagingParameter setPerPage(int perPage) {
		this.perPage = perPage;
		return this;

	}

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
	public PagingParameter setSortOrder(String sortBy) {
		this.sortBy = sortBy;
		return this;
	}

	/**
	 * Return the sort by parameter value.
	 * 
	 * @return Field to be sorted by
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public String getSortBy() {
		return sortBy;
	}

	/**
	 * Return the order parameter value.
	 * 
	 * @return Sort order
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public SortOrder getOrder() {
		return order;
	}

	@Override
	public String getQueryParameters() {
		//TODO add the other parameters as well
		return PAGE_PARAMETER_KEY + "=" + page + "&" + PER_PAGE_PARAMETER_KEY + "=" + perPage;
	}

}
