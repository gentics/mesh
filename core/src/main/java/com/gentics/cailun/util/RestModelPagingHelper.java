package com.gentics.cailun.util;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;
import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.common.response.PagingMetaInfo;

public final class RestModelPagingHelper {

	public static void setPaging(AbstractListResponse<? extends AbstractRestModel> response, long currentPage, long pageCount, long perPage,
			long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);
	}
}
