package com.gentics.mesh.plugin.keycloak;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.testcontainers.containers.wait.strategy.Wait;

import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;
import com.gentics.mesh.test.docker.KeycloakContainer;

public class Keycloak26PluginTest extends AbstractKeycloakPluginTest {

	static {
		realmUrlPrefix = StringUtils.EMPTY;
	}

	@SuppressWarnings("resource")
	@ClassRule
	public static final TestRule initKeycloak() {
		keycloak = new KeycloakContainer("/keycloak/realm.json", "keycloak/keycloak", "26.1.0", Arrays.asList("start-dev", "-Dkeycloak.migration.dir=/realms", "--import-realm"), true)
				.waitingFor(Wait.forHttp("/realms/master-test"));
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
