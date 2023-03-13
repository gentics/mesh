
package com.gentics.mesh.core.graphql.javafilter;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;

import com.gentics.mesh.core.graphql.GraphQLNodeSearchEndpointTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLNodeSearchEndpointTest extends GraphQLNodeSearchEndpointTest {

}
