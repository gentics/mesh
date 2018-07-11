package com.gentics.mesh.search;

import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Test;

import static com.gentics.mesh.test.TestSize.FULL;
import static junit.framework.TestCase.assertTrue;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class SearchStatusTest extends AbstractMeshTest {
	@Test
	public void testAvailable() {
		Boolean available = client().searchStatus().toSingle()
			.map(SearchStatusResponse::isAvailable)
			.blockingGet();

		assertTrue("Search should be available", available);
	}
}
