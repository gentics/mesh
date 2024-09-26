package com.gentics.mesh.core.graphql.nativefilter;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLWaitSearchEndpointTest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;

@Category({ NativeGraphQLFilterTests.class })
@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLWaitSearchEndpointTest extends GraphQLWaitSearchEndpointTest {

	public NativeGraphQLWaitSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}
}
