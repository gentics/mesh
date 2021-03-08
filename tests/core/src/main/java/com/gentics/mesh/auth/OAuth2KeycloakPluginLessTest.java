package com.gentics.mesh.auth;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Test functionality of the default mapper when no plugin is in use.
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true, useKeycloak = true)
public class OAuth2KeycloakPluginLessTest extends AbstractOAuthTest {

	@Test
	public void testDefaultMapper() throws IOException {
		setClientTokenFromKeycloak();
		UserResponse me = call(() -> client().me());
		assertEquals("dummy@dummy.dummy", me.getEmailAddress());
		assertEquals("Dummy", me.getFirstname());
		assertEquals("User", me.getLastname());
		assertEquals("dummyuser", me.getUsername());
	}
}
