package com.gentics.mesh.query.impl;

import static com.gentics.mesh.util.HttpQueryUtils.splitQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import java.util.Map;

public class PagingParametersTest {

	@Test
	public void testParameter() throws Exception {
		PagingParametersImpl params = new PagingParametersImpl();
		assertNull("Initially no order should be set", params.getOrder());
		assertEquals("By default sortorder should be by uuid.", "uuid", params.getSortBy());
		assertEquals("The default page is one but the method did not return one.", 1, params.getPage());
		assertNull(params.getPerPage());

		Map<String, String> paramMap = splitQuery(params.getQueryParameters());

		assertEquals(2, paramMap.size());
		assertEquals("1", paramMap.get("page"));
		assertEquals("uuid", paramMap.get("sortBy"));

		params.setPerPage(25L);
		paramMap = splitQuery(params.getQueryParameters());

		assertEquals(3, paramMap.size());
		assertEquals("1", paramMap.get("page"));
		assertEquals("uuid", paramMap.get("sortBy"));
		assertEquals("25", paramMap.get("perPage"));
	}
}
