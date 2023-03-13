package com.gentics.mesh.core.graphql.nativefilter;

import com.gentics.mesh.core.graphql.GraphQLMeshTypeTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLMeshTypeTest extends GraphQLMeshTypeTest {

}
