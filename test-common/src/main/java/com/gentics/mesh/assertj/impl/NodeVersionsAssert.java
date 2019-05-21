package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;

public class NodeVersionsAssert extends AbstractAssert<NodeVersionsAssert, NodeVersionsResponse> {

	public NodeVersionsAssert(NodeVersionsResponse actual) {
		super(actual, NodeVersionsAssert.class);
	}

	public NodeVersionsAssert hasVersions(String expected) {
		assertEquals("The versions don't match up.", expected, actual.toString());
		return this;
	}
}
