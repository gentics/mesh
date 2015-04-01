package com.gentics.cailun.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.cailun.core.rest.common.response.AbstractListResponse;

public class RestModelPagingHelperTest {

	@Test
	public void testPagingHelper() {
		TestListResponse response = new TestListResponse();
		RestModelPagingHelper.setPaging(response, "/test", 4, 10, 200, 10 * 200);

		assertNotNull(response.getMetainfo());
		assertEquals(4, response.getMetainfo().getCurrentPage());
		assertEquals(10, response.getMetainfo().getPageCount());
		assertEquals(10 * 200, response.getMetainfo().getTotalCount());
		assertEquals(200, response.getMetainfo().getPerPage());

//		assertEquals("/test?page=1&per_page=200", response.getMetainfo().getLinks().getFirst());
//		assertEquals("/test?page=10&per_page=200", response.getMetainfo().getLinks().getLast());
//		assertEquals("/test?page=5&per_page=200", response.getMetainfo().getLinks().getNext());
//		assertEquals("/test?page=3&per_page=200", response.getMetainfo().getLinks().getPrevious());
//		assertEquals("/test?page=4&per_page=200", response.getMetainfo().getLinks().getSelf());
	}
}

class TestListResponse extends AbstractListResponse {

}
