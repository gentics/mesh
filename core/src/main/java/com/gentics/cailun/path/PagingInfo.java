package com.gentics.cailun.path;

public class PagingInfo {

	private long page;
	private long perPage;

	public PagingInfo(long page, long perPage) {
		this.page = page;
		this.perPage = perPage;
	}

	public long getPage() {
		return page;
	}

	public long getPerPage() {
		return perPage;
	}

}
