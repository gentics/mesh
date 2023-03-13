package com.gentics.mesh.core.graphql.javafilter;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLUserScrollSearchEndpointTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
@RunWith(Parameterized.class)
public class JavaGraphQLUserScrollSearchEndpointTest extends GraphQLUserScrollSearchEndpointTest {

	public JavaGraphQLUserScrollSearchEndpointTest(String queryName) {
		super(queryName);
	}
}
