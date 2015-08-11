package com.gentics.mesh.core.rest.common;

/**
 * Paging meta info model.
 */
public class PagingMetaInfo {

	private long currentPage;

	private long perPage;

	private long pageCount;

	private long totalCount;

	public PagingMetaInfo() {
	}

	public long getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(long currentPage) {
		this.currentPage = currentPage;
	}

	public long getPerPage() {
		return perPage;
	}

	public void setPerPage(long perPage) {
		this.perPage = perPage;
	}

	public long getPageCount() {
		return pageCount;
	}

	public void setPageCount(long pageCount) {
		this.pageCount = pageCount;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

}
