package com.gentics.mesh.core.graphql.nativefilter;

import java.util.function.Consumer;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLEndpointTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;

import io.vertx.core.json.JsonObject;

@Category({ NativeGraphQLFilterTests.class })
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLEndpointTest extends GraphQLEndpointTest {

	public NativeGraphQLEndpointTest(String queryName, boolean withMicroschema, boolean withBranchPathPrefix, String version, Consumer<JsonObject> assertion, String apiVersion) {
		super(queryName, withMicroschema, withBranchPathPrefix, version, assertion, apiVersion);
	}
}
