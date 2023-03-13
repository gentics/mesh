package com.gentics.mesh.core.graphql.nativefilter;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLNodeScrollSearchEndpointTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
@RunWith(Parameterized.class)
public class NativeGraphQLNodeScrollSearchEndpointTest extends GraphQLNodeScrollSearchEndpointTest {

	public NativeGraphQLNodeScrollSearchEndpointTest(String queryName) {
		super(queryName);
	}

}
