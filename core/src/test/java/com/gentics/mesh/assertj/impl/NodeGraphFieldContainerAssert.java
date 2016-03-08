package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

public class NodeGraphFieldContainerAssert extends AbstractAssert<NodeGraphFieldContainerAssert, NodeGraphFieldContainer> {

	public NodeGraphFieldContainerAssert(NodeGraphFieldContainer actual) {
		super(actual, NodeGraphFieldContainerAssert.class);
	}

	/**
	 * Assert that the field uses the given schema.
	 * 
	 * @param schemaVersion
	 *            schema container
	 * @return fluent API
	 */
	public NodeGraphFieldContainerAssert isOf(SchemaContainerVersion schemaVersion) {
		assertEquals("The schema uuid of the field container {" + actual.getUuid() + "} did not match the expected one.", schemaVersion,
				actual.getSchemaContainerVersion());
		return this;
	}
}
