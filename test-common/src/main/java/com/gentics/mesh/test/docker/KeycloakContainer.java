package com.gentics.mesh.test.docker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.Wait;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

	private static final String VERSION = "3.4.3.Final";

	private static final Logger log = LoggerFactory.getLogger(KeycloakContainer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private String realmPath = null;

	public KeycloakContainer() {
		super("jboss/keycloak:" + VERSION);
	}

	@Override
	protected void configure() {
		addEnv("KEYCLOAK_USER", "admin");
		addEnv("KEYCLOAK_PASSWORD", "admin");

		setExposedPorts(Arrays.asList(8080));
		setLogConsumers(Arrays.asList(logConsumer));
		setStartupAttempts(1);

		List<String> args = new ArrayList<>();
		args.add("-b");
		args.add("0.0.0.0");
		args.add("-Dkeycloak.migration.usersExportStrategy=SAME_FILE");
		args.add("-Dkeycloak.migration.strategy=OVERWRITE_EXISTING");

		if (realmPath != null) {
			String containerPath = "/opt/jboss/keycloak/realm.json";
			addFileSystemBind(new File(realmPath).getAbsolutePath(), containerPath, BindMode.READ_ONLY);
			args.add("-Dkeycloak.import=" + containerPath);
		}
		withCommand(args.stream().toArray(String[]::new));
		
	}

	public KeycloakContainer waitStartup() {
		//started in 
		waitingFor(Wait.forListeningPort());
		return this;
	}

	public KeycloakContainer withRealmFile(String path) {
		this.realmPath = path;
		return this;
	}
}
