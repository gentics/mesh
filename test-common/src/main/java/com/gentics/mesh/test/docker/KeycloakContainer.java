package com.gentics.mesh.test.docker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import io.vertx.core.json.JsonObject;

/**
 * Docker Testcontainer for keycloak.
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

	private static final String VERSION = "6.0.1";

	private static final Logger log = LoggerFactory.getLogger(KeycloakContainer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private static final String REALM_CONFIG_PATH = "/opt/jboss/keycloak/realm.json";

	private boolean withConfig = false;

	public KeycloakContainer(String classPathToConfig) {
		super(prepareDockerImage(loadConfig(classPathToConfig)));
		this.withConfig = true;
	}

	@Override
	protected void configure() {
		addEnv("KEYCLOAK_USER", "admin");
		addEnv("KEYCLOAK_PASSWORD", "admin");

		withExposedPorts(8080);
		withLogConsumer(logConsumer);
		setStartupAttempts(1);

		List<String> args = new ArrayList<>();
		args.add("-b");
		args.add("0.0.0.0");
		args.add("-Dkeycloak.migration.usersExportStrategy=SAME_FILE");
		args.add("-Dkeycloak.migration.strategy=OVERWRITE_EXISTING");

		if (withConfig) {
			args.add("-Dkeycloak.import=" + REALM_CONFIG_PATH);
		}
		withCommand(args.stream().toArray(String[]::new));
	}

	public static ImageFromDockerfile prepareDockerImage(JsonObject realmConfig) {
		ImageFromDockerfile dockerImage = new ImageFromDockerfile("keycloak-mesh", true);
		StringBuilder dockerFile = new StringBuilder();
		dockerFile.append("FROM jboss/keycloak:" + VERSION + "\n");

		if (realmConfig != null) {
			dockerFile.append("ADD /realm.json " + REALM_CONFIG_PATH + "\n");
			dockerImage.withFileFromString("/realm.json", realmConfig.encodePrettily());
		}
		dockerImage.withFileFromString("/Dockerfile", dockerFile.toString());
		return dockerImage;
	}

	/**
	 * Wait until the keycloak port is reachable.
	 * 
	 * @return
	 */
	public KeycloakContainer waitStartup() {
		waitingFor(Wait.forListeningPort());
		return this;
	}

	public static JsonObject loadConfig(String classPath) {
		try (InputStream ins = KeycloakContainer.class.getResourceAsStream(classPath)) {
			Objects.requireNonNull(ins, "Could not find file {" + classPath + "}");
			String json = IOUtils.toString(ins, StandardCharsets.UTF_8);
			return new JsonObject(json);
		} catch (IOException e) {
			throw new RuntimeException("Could not loadrealm server config file.", e);
		}
	}

	public String getHost() {
		String containerHost = System.getenv("CONTAINER_HOST");
		if (containerHost != null) {
			return containerHost;
		} else {
			return "localhost";
		}
	}
}
