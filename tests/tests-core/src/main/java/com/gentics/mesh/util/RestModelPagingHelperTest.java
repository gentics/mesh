package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

public class RestModelPagingHelperTest {

	@Test
	public void testPagingOffsetCorrection() {
		ListResponse<?> response = new ListResponse<>();
		Page<?> page = mock(PageImpl.class);

		long nPages = 3;
		long nCurrentPage = 0;
		long nTotalElements = 1000;
		long nPageSize = 21;

		PagingParametersImpl info = new PagingParametersImpl((int) (nCurrentPage + 1), nPageSize);

		when(page.getNumber()).thenReturn(nCurrentPage);
		when(page.getPageCount()).thenReturn(nPages);
		when(page.getTotalElements()).thenReturn(nTotalElements);
		when(page.getPerPage()).thenReturn(nPageSize);
		Mockito.doCallRealMethod().when(page).setPaging(response);
		page.setPaging(response);

		assertNotNull(response.getMetainfo());
		assertEquals(nCurrentPage, response.getMetainfo().getCurrentPage());
		assertEquals(nPages, response.getMetainfo().getPageCount());
		assertEquals(nTotalElements, response.getMetainfo().getTotalCount());
		assertEquals(info.getPerPage(), response.getMetainfo().getPerPage());
	}

}
