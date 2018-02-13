package com.gentics.mesh.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;

public class NodeExamplesTest {

	@Test
	public void testNodeExample() {
		NodeResponse response = new NodeExamples().getNodeResponseWithAllFields();
		assertNotNull(response.getUuid());
		assertThat(response.getTags()).isNotEmpty();
		assertThat(response.getBreadcrumb()).isNotEmpty();
		assertNotNull(response.toJson());
	}
}
