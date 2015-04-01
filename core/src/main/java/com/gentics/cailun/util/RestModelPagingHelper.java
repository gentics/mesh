package com.gentics.cailun.util;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;
import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.common.response.PagingLinks;
import com.gentics.cailun.core.rest.common.response.PagingMetaInfo;

public final class RestModelPagingHelper {

	public static void setPaging(AbstractListResponse<? extends AbstractRestModel> response, String path, long currentPage, long pageCount, long perPage,
			long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);

//		PagingLinks links = info.getLinks();
//		links.setFirst(path + "?page=1&per_page=" + info.getPerPage());
//		links.setSelf(path + "?page=" + info.getCurrentPage() + "&per_page=" + info.getPerPage());
//		long previousPage = info.getCurrentPage() == 1 ? 1 : info.getCurrentPage() - 1;
//		links.setPrevious(path + "?page=" + previousPage + "&per_page=" + info.getPerPage());
//		long nextPage = info.getCurrentPage() == info.getPageCount() ? info.getPageCount() : info.getCurrentPage() + 1;
//		links.setNext(path + "?page=" + nextPage + "&per_page=" + info.getPerPage());
//		links.setLast(path + "?page=" + info.getPageCount() + "&per_page=" + info.getPerPage());

	}
}
