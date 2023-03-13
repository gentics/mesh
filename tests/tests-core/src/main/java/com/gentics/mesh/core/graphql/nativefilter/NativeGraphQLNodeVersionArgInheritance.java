package com.gentics.mesh.core.graphql.nativefilter;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLNodeVersionArgInheritance;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

/**
 * This test will verify that the inheritance mechanism for the node type argument works as expected.
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLNodeVersionArgInheritance extends GraphQLNodeVersionArgInheritance {

	public NativeGraphQLNodeVersionArgInheritance(String version) {
		super(version);
	}
}
