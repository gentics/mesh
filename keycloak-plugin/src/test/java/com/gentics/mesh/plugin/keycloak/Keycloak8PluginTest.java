package com.gentics.mesh.plugin.keycloak;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.rules.TestRule;
import org.testcontainers.containers.wait.strategy.Wait;

import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;
import com.gentics.mesh.test.docker.KeycloakContainer;

@Ignore("Target image unavailable")
public class Keycloak8PluginTest extends AbstractKeycloakPluginTest {

	static {
		realmUrlPrefix = "/auth";
	}

	@SuppressWarnings("resource")
	@ClassRule
	public static final TestRule initKeycloak() {
		keycloak = new KeycloakContainer("/keycloak/realm.json", "8.0.1")
				.waitingFor(Wait.forHttp("/auth/realms/master-test"));
		return keycloak;
	} ;

	@BeforeClass
	public static void configurePlugin() {
		KeycloakTestPlugin.config = new KeycloakPluginConfig()
			.setRealmName("master-test")
			.setRealmUrlPrefix(realmUrlPrefix)
			.setUrl("http://" + keycloak.getContainerIpAddress() + ":" + keycloak.getFirstMappedPort())
			.setLoadPublicKeys(true);
	}
}
