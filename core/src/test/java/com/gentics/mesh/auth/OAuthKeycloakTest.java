package com.gentics.mesh.auth;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true, useKeycloak = true)
public class OAuthKeycloakTest extends AbstractMeshTest {

	@Test
	public void testKeycloakAuth() {
		client().logout();
		UserResponse me = call(() -> client().me());
		System.out.println(me.toJson());
	}
}
