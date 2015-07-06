package com.gentics.mesh.api.common;


public class PagingInfo {

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

	public PagingInfo(int page, int perPage) {
		this(page, perPage, null, SortOrder.UNSORTED);
	}

	public int getPage() {
		return page;
	}

	public int getPerPage() {
		return perPage;
	}

	public String getSortBy() {
		return sortBy;
	}

	public SortOrder getOrder() {
		return order;
	}

}
