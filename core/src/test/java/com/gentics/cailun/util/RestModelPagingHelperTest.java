package com.gentics.cailun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.springframework.data.domain.Page;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;
import com.gentics.cailun.paging.PagingInfo;

public class RestModelPagingHelperTest {

	@Test
	public void testPagingOffsetCorrection() {
		TestListResponse response = new TestListResponse();
		Page<?> page = mock(Page.class);
		
		int nSDNPages = 3;
		int nSDNCurrentPage = 0;
		int nSDNTotalItems = 1000;
		long nSDNPageSize = 21;

		PagingInfo info = new PagingInfo(nSDNCurrentPage + 1, (int) nSDNPageSize);

		when(page.getNumber()).thenReturn(0);
		when(page.getTotalPages()).thenReturn(nSDNPages);
		when(page.getNumberOfElements()).thenReturn(nSDNTotalItems);
		when(page.getTotalElements()).thenReturn(nSDNPageSize);

		RestModelPagingHelper.setPaging(response, page, info);

		assertNotNull(response.getMetainfo());
		assertEquals(nSDNCurrentPage + 1, response.getMetainfo().getCurrentPage());
		assertEquals(nSDNPages, response.getMetainfo().getPageCount());
		assertEquals(nSDNPageSize, response.getMetainfo().getTotalCount());
		assertEquals(info.getPerPage(), response.getMetainfo().getPerPage());
	}

}

class TestListResponse extends AbstractListResponse {

}
