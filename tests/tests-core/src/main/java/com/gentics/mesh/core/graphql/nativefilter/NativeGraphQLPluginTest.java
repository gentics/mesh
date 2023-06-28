package com.gentics.mesh.core.graphql.nativefilter;

import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.graphql.GraphQLPluginTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;
import com.gentics.mesh.test.category.PluginTests;

@Category({PluginTests.class, NativeGraphQLFilterTests.class})
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLPluginTest extends GraphQLPluginTest {

}
