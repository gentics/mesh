package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.query.impl.PagingParameter;
public class RestModelPagingHelperTest {

	@Test
	public void testPagingOffsetCorrection() {
		TestListResponse response = new TestListResponse();
		Page<?> page = mock(Page.class);

		int nPages = 3;
		int nCurrentPage = 0;
		int nTotalElements = 1000;
		int nElements = 200;
		long nPageSize = 21;

		PagingParameter info = new PagingParameter(nCurrentPage + 1, (int) nPageSize);

		when(page.getNumber()).thenReturn(nCurrentPage);
		when(page.getTotalPages()).thenReturn(nPages);
		when(page.getNumberOfElements()).thenReturn(nElements);
		when(page.getTotalElements()).thenReturn(nTotalElements);
		when(page.getPerPage()).thenReturn(nPageSize);

		page.setPaging(response);

		assertNotNull(response.getMetainfo());
		assertEquals(nCurrentPage, response.getMetainfo().getCurrentPage());
		assertEquals(nPages, response.getMetainfo().getPageCount());
		assertEquals(nTotalElements, response.getMetainfo().getTotalCount());
		assertEquals(info.getPerPage(), response.getMetainfo().getPerPage());
	}

}

@SuppressWarnings("rawtypes")
class TestListResponse extends AbstractListResponse {

}
