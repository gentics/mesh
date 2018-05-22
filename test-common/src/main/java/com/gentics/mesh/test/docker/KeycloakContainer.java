package com.gentics.mesh.test.docker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.Wait;

import io.vertx.core.json.JsonObject;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

	private static final String REALM_FILE_NAME = "realm.json";

	private static final String VERSION = "3.4.3.Final";

	private static final Logger log = LoggerFactory.getLogger(KeycloakContainer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	/**
	 * Path to the realm configuration on the docker host system.
	 */
	private String realmHostPath = null;

	private JsonObject json;

	public KeycloakContainer() {
		super("jboss/keycloak:" + VERSION);
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

		if (json != null) {
			realmHostPath = toHostPath(json).getAbsolutePath();
		}

		if (realmHostPath != null) {
			String containerPath = "/opt/jboss/keycloak/realm.json";
			addFileSystemBind(realmHostPath, containerPath, BindMode.READ_ONLY);
			args.add("-Dkeycloak.import=" + containerPath);
		}
		withCommand(args.stream().toArray(String[]::new));
	}

	/**
	 * Write the json to a temp file and return the file.
	 * 
	 * @param json
	 * @return Path to created file which contains the json data
	 */
	private File toHostPath(JsonObject json) {
		try {
			File hostPath = File.createTempFile("realm-file", ".json");
			FileUtils.writeStringToFile(hostPath, json.encodePrettily(), Charset.defaultCharset());
			return hostPath;
		} catch (IOException e) {
			throw new RuntimeException("Could not create realm host file", e);
		}
	}

	public KeycloakContainer waitStartup() {
		waitingFor(Wait.forListeningPort());
		return this;
	}

	public KeycloakContainer withRealmConfig(JsonObject json) {
		this.json = json;
		return this;
	}

	public KeycloakContainer withRealmFromClassPath(String path) {
		JsonObject realmServerConfig;

		try (InputStream ins = getClass().getResourceAsStream(path)) {
			Objects.requireNonNull(ins, "Could not find file {" + path + "}");
			String json = IOUtils.toString(ins, Charset.defaultCharset());
			realmServerConfig = new JsonObject(json);
		} catch (IOException e) {
			throw new RuntimeException("Could not write realm server config file.", e);
		}
		withRealmConfig(realmServerConfig);
		return this;
	}
}
