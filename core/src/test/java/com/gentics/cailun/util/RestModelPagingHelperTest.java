package com.gentics.cailun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;

public class RestModelPagingHelperTest {

	@Test
	public void testPagingHelper() {
		TestListResponse response = new TestListResponse();
		RestModelPagingHelper.setPaging(response, 4, 10, 200, 10 * 200);

		assertNotNull(response.getMetainfo());
		assertEquals(4, response.getMetainfo().getCurrentPage());
		assertEquals(10, response.getMetainfo().getPageCount());
		assertEquals(10 * 200, response.getMetainfo().getTotalCount());
		assertEquals(200, response.getMetainfo().getPerPage());
	}
}

class TestListResponse extends AbstractListResponse {

}
