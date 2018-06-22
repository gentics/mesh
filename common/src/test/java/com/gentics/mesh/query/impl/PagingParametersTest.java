package com.gentics.mesh.query.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.PagingParametersImpl;

public class PagingParametersTest {

	@Test
	public void testParameter() throws Exception {
		PagingParametersImpl params = new PagingParametersImpl();
		assertNull("Initially no order should be set", params.getOrder());
		assertEquals("By default sortorder should be by uuid.", "uuid", params.getSortBy());
		assertEquals("The default page is one but the method did not return one.", 1, params.getPage());
		assertNull(params.getPerPage());

		assertEquals("page=1&perPage=25&sortBy=uuid", params.getQueryParameters());

	}
}
