package com.gentics.mesh.core.graphql.javafilter;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLNodeLanguageSearchEnpointTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLNodeLanguageSearchEnpointTest extends GraphQLNodeLanguageSearchEnpointTest {
	public JavaGraphQLNodeLanguageSearchEnpointTest(String queryName) {
		super(queryName);
	}
}
