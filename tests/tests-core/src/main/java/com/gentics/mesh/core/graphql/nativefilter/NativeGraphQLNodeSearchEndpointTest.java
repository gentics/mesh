
package com.gentics.mesh.core.graphql.nativefilter;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;

import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.graphql.GraphQLNodeSearchEndpointTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;

@Category({ NativeGraphQLFilterTests.class })
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLNodeSearchEndpointTest extends GraphQLNodeSearchEndpointTest {

}
