package com.gentics.mesh.search.raw;


import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, startServer = true, testSize = FULL)
public class MicroschemaRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {
		// TODO Not yet implemented
	}
}
