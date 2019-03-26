package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class SearchStatusTest extends AbstractMeshTest {
	@Test
	public void testAvailable() {
		Boolean available = client().searchStatus().toSingle()
			.map(SearchStatusResponse::isAvailable)
			.blockingGet();

		assertTrue("Search should be available", available);
	}
}
