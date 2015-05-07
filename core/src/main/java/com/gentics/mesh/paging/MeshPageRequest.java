package com.gentics.mesh.paging;

import org.springframework.data.domain.PageRequest;

public class MeshPageRequest extends PageRequest {

	public MeshPageRequest(int page, int size) {
		super(page, size);
	}

	public MeshPageRequest(PagingInfo pagingInfo) {
		super(pagingInfo.getPage() - 1, pagingInfo.getPerPage());
	}
}
