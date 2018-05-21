package com.gentics.mesh.auth;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.test.docker.KeycloakContainer;

public class OAuthKeycloakTest {

	@ClassRule
	public static KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmFile("src/test/resources/realm.json")
		.waitStartup();
	
	@Test
	public void testKeycloakAuth() {
		
	}
}
