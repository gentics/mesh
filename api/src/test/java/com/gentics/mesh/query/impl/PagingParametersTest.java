package com.gentics.mesh.query.impl;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.PagingParameters;

public class PagingParametersTest {

	@Test
	public void testParameter() throws Exception {
		PagingParameters params = new PagingParameters();
		assertNull("Initially no order should be set", params.getOrder());
		assertEquals("By default sortorder should be by uuid.", "uuid", params.getSortBy());
		assertEquals("The default page is one but the method did not return one.", 1, params.getPage());
		assertEquals("Initially the default per page size should be set", 25, params.getPerPage());

		assertEquals("page=1&perPage=25&sortBy=uuid", params.getQueryParameters());

	}
}
