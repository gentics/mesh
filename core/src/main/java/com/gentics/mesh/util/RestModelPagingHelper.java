package com.gentics.mesh.util;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;

public final class RestModelPagingHelper {

	public static void setPaging(AbstractListResponse<?> response, Page<?> page, PagingInfo pagingInfo) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(page.getNumber());
		info.setPageCount(page.getTotalPages());
		info.setPerPage(pagingInfo.getPerPage());
		info.setTotalCount(page.getTotalElements());
	}
}
