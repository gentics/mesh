package com.gentics.mesh.core.graphql.javafilter;

import com.gentics.mesh.core.graphql.GraphQLMeshTypeTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLMeshTypeTest extends GraphQLMeshTypeTest {

}
