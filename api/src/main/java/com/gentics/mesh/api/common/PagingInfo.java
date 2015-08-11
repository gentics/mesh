package com.gentics.mesh.api.common;

import com.gentics.mesh.core.rest.node.QueryParameterProvider;

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

	public int getPage() {
		return page;
	}

	public int getPerPage() {
		return perPage;
	}

	public PagingInfo setPerPage(int perPage) {
		this.perPage = perPage;
		return this;

	}

	public PagingInfo setPage(int page) {
		this.page = page;
		return this;
	}

	public PagingInfo setSortOrder(String sortBy) {
		this.sortBy = sortBy;
		return this;
	}

	public String getSortBy() {
		return sortBy;
	}

	public SortOrder getOrder() {
		return order;
	}

	@Override
	public String getQueryParameters() {
		//TODO add the other parameters as well
		return "page=" + page + "&perPage=" + perPage;
	}

}
