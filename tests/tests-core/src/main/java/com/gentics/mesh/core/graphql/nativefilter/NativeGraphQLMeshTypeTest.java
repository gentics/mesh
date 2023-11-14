package com.gentics.mesh.core.graphql.nativefilter;

import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.graphql.GraphQLMeshTypeTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;

@Category({ NativeGraphQLFilterTests.class })
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLMeshTypeTest extends GraphQLMeshTypeTest {

}