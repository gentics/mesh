package com.gentics.mesh.core.graphql.javafilter;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLNodePermissionTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLNodePermissionTest extends GraphQLNodePermissionTest {

	public JavaGraphQLNodePermissionTest(PermissionScenario perm, ContentSetupType setup) {
		super(perm, setup);
	}
}
