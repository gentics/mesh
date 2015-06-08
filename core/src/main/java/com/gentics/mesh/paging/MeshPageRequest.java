package com.gentics.mesh.paging;

public class MeshPageRequest {

	int page;
	int size;

	public MeshPageRequest(int page, int size) {
		this.page = page;
		this.size = size;
	}

	public MeshPageRequest(PagingInfo pagingInfo) {
		this(pagingInfo.getPage() - 1, pagingInfo.getPerPage());
	}
}
