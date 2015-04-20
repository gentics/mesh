package com.gentics.cailun.paging;

import org.springframework.data.domain.PageRequest;

public class CaiLunPageRequest extends PageRequest {

	public CaiLunPageRequest(int page, int size) {
		super(page, size);
	}

	public CaiLunPageRequest(PagingInfo pagingInfo) {
		super(pagingInfo.getPage() - 1, pagingInfo.getPerPage());
	}
}
