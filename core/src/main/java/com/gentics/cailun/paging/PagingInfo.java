package com.gentics.cailun.paging;

public class PagingInfo {

	private int page;
	private int perPage;

	public PagingInfo(int page, int perPage) {
		this.page = page;
		this.perPage = perPage;
	}

	public int getPage() {
		return page;
	}

	public int getPerPage() {
		return perPage;
	}

}
