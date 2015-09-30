package com.gentics.mesh.api.common;

import com.gentics.mesh.core.rest.node.QueryParameterProvider;

/**
 * A {@link PagingInfo} can be used to add additional paging parameters to the rest requests.
 */
public class PagingInfo implements QueryParameterProvider {

	private int page;
	private int perPage;
	private String sortBy;
	private SortOrder order;

	public PagingInfo(int page, int perPage, String sortBy, SortOrder order) {
		this.page = page;
		this.perPage = perPage;
		this.sortBy = sortBy;
		this.order = order;
	}

	public PagingInfo() {
		this(1);
	}

	public PagingInfo(int page) {
		//TODO use reference for default page size
		this(page, 25);
	}

	public PagingInfo(int page, int perPage) {
		this(page, perPage, null, SortOrder.UNSORTED);
	}

	/**
	 * Return the current page.
	 * 
	 * @return
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Return the per page count.
	 * 
	 * @return
	 */
	public int getPerPage() {
		return perPage;
	}

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 * @return
	 */
	public PagingInfo setPerPage(int perPage) {
		this.perPage = perPage;
		return this;

	}

	/**
	 * Set the current page.
	 * 
	 * @param page
	 * @return Fluent API
	 */
	public PagingInfo setPage(int page) {
		this.page = page;
		return this;
	}

	/**
	 * Set the used sortorder.
	 * 
	 * @param sortBy
	 * @return Fluent API
	 * @deprecated not yet implemented
	 * 
	 */
	@Deprecated
	public PagingInfo setSortOrder(String sortBy) {
		this.sortBy = sortBy;
		return this;
	}

	/**
	 * Return the sort by parameter value.
	 * 
	 * @return
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public String getSortBy() {
		return sortBy;
	}

	/**
	 * Return the order parameter value.
	 * 
	 * @return
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public SortOrder getOrder() {
		return order;
	}

	@Override
	public String getQueryParameters() {
		//TODO add the other parameters as well
		return "page=" + page + "&perPage=" + perPage;
	}

}
