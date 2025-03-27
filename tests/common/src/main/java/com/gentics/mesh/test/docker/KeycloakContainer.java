package com.gentics.mesh.test.docker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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

	private static final String REALM_CONFIG_PATH = "/realms";
	private static final String REALM_CONFIG_NAME = "/realm.json";

	private final boolean withConfig;
	private final boolean withConfigFolder;
	private final List<String> customArgs;

	public KeycloakContainer(String classPathToConfig, String containerName, String keycloakVersion, List<String> customArgs, boolean withConfigFolder) {
		super(prepareDockerImage(loadConfig(classPathToConfig), containerName, keycloakVersion));
		this.withConfig = true;
		this.withConfigFolder = withConfigFolder;
		this.customArgs = customArgs;
	}

	public KeycloakContainer(String classPathToConfig, String keycloakVersion) {
		super(prepareDockerImage(loadConfig(classPathToConfig), "jboss/keycloak", keycloakVersion));
		this.withConfig = true;
		this.withConfigFolder = false;
		this.customArgs = Arrays.asList("-b", "0.0.0.0");
	}

	public KeycloakContainer(String classPathToConfig) {
		this(classPathToConfig, VERSION);
	}

	@Override
	protected void configure() {
		addEnv("KEYCLOAK_USER", "admin");
		addEnv("KEYCLOAK_PASSWORD", "admin");

		withExposedPorts(8080);
		withLogConsumer(logConsumer);
		withStartupTimeout(Duration.ofMinutes(5));
		setStartupAttempts(1);

		List<String> args = new ArrayList<>();
		customArgs.stream().forEach(args::add);
		args.add("-Dkeycloak.migration.usersExportStrategy=SAME_FILE");
		args.add("-Dkeycloak.migration.strategy=OVERWRITE_EXISTING");

		if (withConfig) {
			args.add("-Dkeycloak.import=" + REALM_CONFIG_PATH + (withConfigFolder ? "" : REALM_CONFIG_NAME));
		}
		withCommand(args.stream().toArray(String[]::new));
	}

	public static ImageFromDockerfile prepareDockerImage(JsonObject realmConfig, String containerName, String version) {
		ImageFromDockerfile dockerImage = new ImageFromDockerfile("keycloak-mesh-" + containerName + "-" + version, true);
		StringBuilder dockerFile = new StringBuilder();
		dockerFile.append("FROM ").append(System.getProperty("mesh.container.image.prefix", "")).append(containerName).append(":").append(version).append("\n");

		if (realmConfig != null) {
			dockerFile.append("ADD ").append(REALM_CONFIG_NAME).append(" ").append(REALM_CONFIG_PATH).append(REALM_CONFIG_NAME).append("\n");
			dockerImage.withFileFromString(REALM_CONFIG_NAME, realmConfig.encodePrettily());
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
