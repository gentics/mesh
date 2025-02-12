package com.gentics.mesh.core.graphql.javafilter;

import com.gentics.mesh.core.graphql.GraphQLEndpointBranchTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLEndpointBranchTest extends GraphQLEndpointBranchTest {
}
