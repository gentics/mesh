package com.gentics.mesh.search.raw;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class MicroschemaRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {
		// TODO Not yet implemented
	}
}
