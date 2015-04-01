package com.gentics.cailun.core.rest.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PagingMetaInfo {

	@JsonProperty("page")
	private long currentPage;

	@JsonProperty("per_page")
	private long perPage;

	@JsonProperty("page_count")
	private long pageCount;

	@JsonProperty("total_count")
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
