package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class SearchStatusTest extends AbstractMultiESTest {

	public SearchStatusTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testAvailable() {
		Boolean available = client().searchStatus().toSingle()
			.map(SearchStatusResponse::isAvailable)
			.blockingGet();

		assertTrue("Search should be available", available);
	}
}
