package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.rest.common.AbstractListResponse;

public class RestModelPagingHelperTest {

	@Test
	public void testPagingOffsetCorrection() {
		TestListResponse response = new TestListResponse();
		Page<?> page = mock(Page.class);

		int nTPPages = 3;
		int nTPCurrentPage = 0;
		int nTPTotalItems = 1000;
		int nTPPageSize = 21;

		PagingInfo info = new PagingInfo(nTPCurrentPage + 1, (int) nTPPageSize);

		when(page.getNumber()).thenReturn(0);
		when(page.getTotalPages()).thenReturn(nTPPages);
		when(page.getNumberOfElements()).thenReturn(nTPTotalItems);
		when(page.getTotalElements()).thenReturn(nTPPageSize);

		RestModelPagingHelper.setPaging(response, page);

		assertNotNull(response.getMetainfo());
		assertEquals(nTPCurrentPage, response.getMetainfo().getCurrentPage());
		assertEquals(nTPPages, response.getMetainfo().getPageCount());
		assertEquals(nTPPageSize, response.getMetainfo().getTotalCount());
		assertEquals(info.getPerPage(), response.getMetainfo().getPerPage());
	}

}

class TestListResponse extends AbstractListResponse {

}
