package com.gentics.mesh.auth.keycloak;

import java.util.Arrays;

import org.testcontainers.containers.GenericContainer;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

	private static final String VERSION = "3.4.3.Final";

	public KeycloakContainer() {
		super("jboss:keycloak:" + VERSION);
	}

	@Override
	protected void configure() {
		addEnv("KEYCLOAK_USER", "admin");
		addEnv("KEYCLOAK_PASSWORD", "admin");

		setExposedPorts(Arrays.asList(8080));
		setStartupAttempts(1);
	}
}
