package com.gentics.mesh.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.etc.config.search.IndexSearchMode;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = TestSize.FULL)
public class IndexSearchModeTest extends NodeSearchEndpointCTest {

	static {
		System.setProperty("MESH_ES_SYNC_FETCH_BATCH_SIZE", Integer.toString(1));
	}

	public IndexSearchModeTest(ElasticsearchTestMode elasticsearch, IndexSearchMode mode) throws Exception {
		super(elasticsearch);
		getTestContext().getOptions().getSearchOptions().setIndexSearchMode(mode);
	}

	@Parameters(name = "{index}: ({0}, {1})")
	public static Collection<Object[]> esVersions() {
		return Arrays.asList(ElasticsearchTestMode.CONTAINER_ES6, ElasticsearchTestMode.CONTAINER_ES7)
			.stream()
			.flatMap(testMode -> Arrays.asList(new Object[] {testMode, IndexSearchMode.SCROLL}, new Object[] {testMode, IndexSearchMode.SEARCH_AFTER}).stream())
			.collect(Collectors.toList());
	}
}
