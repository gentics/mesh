package com.gentics.mesh.util;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.rest.common.response.AbstractListResponse;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.common.response.PagingMetaInfo;
import com.gentics.mesh.paging.PagingInfo;

public final class RestModelPagingHelper {

	public static void setPaging(AbstractListResponse<? extends AbstractRestModel> response, Page<?> page, PagingInfo pagingInfo) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(page.getNumber() + 1);
		info.setPageCount(page.getTotalPages());
		info.setPerPage(pagingInfo.getPerPage());
		info.setTotalCount(page.getTotalElements());
	}
}
