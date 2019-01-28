package com.gentics.mesh.query.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;

public class NodeParametersTest {

	@Test
	public void testNodeParams() {
		NodeParametersImpl params = new NodeParametersImpl();

		// ExpandAll
		assertFalse("The default expandAll setting should be false", params.getExpandAll());
		params.setExpandAll(true);
		assertTrue("The value should be true since we set it to true", params.getExpandAll());

		// Field Names
		assertTrue("The list should be empty", params.getExpandedFieldnameList().isEmpty());
		assertEquals("The array should be empty", 0, params.getExpandedFieldNames().length);
		assertEquals("The method did not return a fluent API", params, params.setExpandedFieldNames("ä", "b", "c"));
		assertThat(params.getExpandedFieldNames()).containsExactly("ä", "b", "c");
		assertThat(params.getExpandedFieldnameList()).containsExactly("ä", "b", "c");

		// Language List
		//assertThat(params.getLanguageList()).containsExactly("en");

		// Resolve Link Type
		assertEquals(LinkType.OFF, params.getResolveLinks());
		assertEquals("The method did not return a fluent API", params, params.setResolveLinks(LinkType.FULL));
		assertEquals("The parameter should have been changed.", LinkType.FULL, params.getResolveLinks());

		assertEquals("expand=ä,b,c&resolveLinks=full&expandAll=true", params.getQueryParameters());
	}


}
