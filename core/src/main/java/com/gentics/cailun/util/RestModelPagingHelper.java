package com.gentics.cailun.util;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;
import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.common.response.PagingMetaInfo;
import com.gentics.cailun.paging.PagingInfo;

public final class RestModelPagingHelper {

	public static void setPaging(AbstractListResponse<? extends AbstractRestModel> response, Page<?> page, PagingInfo pagingInfo) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(page.getNumber() + 1);
		info.setPageCount(page.getTotalPages());
		info.setPerPage(pagingInfo.getPerPage());
		info.setTotalCount(page.getTotalElements());
	}
}
