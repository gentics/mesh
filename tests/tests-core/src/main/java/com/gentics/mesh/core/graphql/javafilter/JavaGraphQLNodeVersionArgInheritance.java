package com.gentics.mesh.core.graphql.javafilter;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLNodeVersionArgInheritance;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

/**
 * This test will verify that the inheritance mechanism for the node type argument works as expected.
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLNodeVersionArgInheritance extends GraphQLNodeVersionArgInheritance {

	public JavaGraphQLNodeVersionArgInheritance(String version) {
		super(version);
	}
}
