package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.etc.config.search.IndexSearchMode;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.UUIDUtil;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = TestSize.FULL)
public class IndexSearchModeTest extends AbstractMultiESTest {

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

	@Before
	public void setup() throws Throwable {
		super.setup();
		NodeResponse node = call(() -> client().findNodeByUuid(projectName(), db().tx(() -> content("concorde").getUuid())));
		IntStream.range(0, 1000).forEach(i -> {
			NodeUpsertRequest upsert = node.toUpsert();
			upsert.getFields().put("content", upsert.getFields().getStringField("content").setString(i + "_content index search test concorde" + System.currentTimeMillis()));
			upsert.getFields().put("slug", upsert.getFields().getStringField("slug").setString(i + "_" + upsert.getFields().getStringField("slug").getString()));
			call(() -> client().upsertNode(projectName(), UUIDUtil.randomUUID(), upsert));
		});
	}

	@Test
	public void testManyResults() throws Exception {
		recreateIndices();
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "concorde"),
				new VersioningParametersImpl().draft()));
			assertEquals(10, response.getData().size());
	}
}
