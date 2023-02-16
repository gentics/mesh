package com.gentics.mesh.query.impl;

import static com.gentics.mesh.util.HttpQueryUtils.splitQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.PagingParametersImpl;

public class PagingParametersTest {

	@Test
	public void testParameter() throws Exception {
		PagingParametersImpl params = new PagingParametersImpl();
		assertTrue("Initially no order should be set", params.getSort().isEmpty());
		assertEquals("The default page is one but the method did not return one.", 1, params.getPage());
		assertNull(params.getPerPage());

		Map<String, String> paramMap = splitQuery(params.getQueryParameters());

		assertEquals(3, paramMap.size());
		assertEquals("1", paramMap.get("page"));

		params.setPerPage(25L);
		paramMap = splitQuery(params.getQueryParameters());

		assertEquals(4, paramMap.size());
		assertEquals("1", paramMap.get("page"));
		assertEquals("25", paramMap.get("perPage"));
	}
}
