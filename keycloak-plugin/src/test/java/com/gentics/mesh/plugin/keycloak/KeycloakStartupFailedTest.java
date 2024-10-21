package com.gentics.mesh.plugin.keycloak;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;

import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;

public class KeycloakStartupFailedTest extends KeycloakStartupDelayedTest {

	@BeforeClass
	public static void configurePlugin() {
		KeycloakTestPlugin.config = new KeycloakPluginConfig()
			.setRetryPeriodSeconds(0) // Fail immediately
			.setRealmName("master-test")
			.setRealmUrlPrefix(realmUrlPrefix)
			.setUrl("http://localhost:" + proxy.actualPort())
			.setLoadPublicKeys(true);
	}

	@Before
	public void setupPlugin() throws InterruptedException {
	}

	@Test(expected = ComparisonFailure.class)
	public void testPlugin() throws IOException {
		try {
			PluginTestUtil.waitForPluginRegistration(server, 5_000, "authPlugin");
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
		fail();
	}
}
