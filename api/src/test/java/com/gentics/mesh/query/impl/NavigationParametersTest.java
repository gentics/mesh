package com.gentics.mesh.query.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.parameter.impl.NavigationParameters;

public class NavigationParametersTest {

	@Test
	@Ignore
	public void testName() throws Exception {
		NavigationParameters params = new NavigationParameters();
		assertNull(params.getMaxDepth());
		params.getQueryParameters();

		assertEquals(params, params.setMaxDepth(10));
		assertEquals(10, params.getMaxDepth().intValue());
		assertEquals("", params.getQueryParameters());

	}
}
