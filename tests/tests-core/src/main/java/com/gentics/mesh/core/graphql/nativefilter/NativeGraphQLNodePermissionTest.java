package com.gentics.mesh.core.graphql.nativefilter;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.graphql.GraphQLNodePermissionTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLNodePermissionTest extends GraphQLNodePermissionTest {

	public NativeGraphQLNodePermissionTest(PermissionScenario perm, ContentSetupType setup) {
		super(perm, setup);
	}
}
