package com.gentics.mesh.plugin.keycloak;

import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.plugin.AbstractCommercialPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.reactivex.Completable;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pf4j.PluginWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;


public class KeycloakPlugin extends AbstractCommercialPlugin<KeycloakPluginConfig> implements AuthServicePlugin {

	private static final String DEFAULT_SCRIPT_NAME = "defaultMapperScript.groovy";
	private static final String MAPPER_SCRIPT_NAME = "mapperScript.groovy";
	private static final Logger log = LoggerFactory.getLogger(KeycloakPlugin.class);
	private Set<JsonObject> publicKeys = Collections.emptySet();
	private String mapperScript;

	public KeycloakPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Set<JsonObject> getPublicKeys() {
		return publicKeys;
	}

	@Override
	public Completable initialize() {
		return loadConfig(KeycloakPluginConfig.class).ignoreElement()
			// TODO Make those two actions non-blocking.
			.andThen(Completable.fromAction(this::loadPublicKeys))
			.andThen(Completable.fromAction(this::loadMapperScript));
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String uuid, JsonObject token) {
		Binding binding = new Binding();

		binding.setVariable("request", req);
		binding.setVariable("uuid", uuid);
		binding.setVariable("token", token);
		binding.setVariable("log", log);

		GroovyShell shell = new GroovyShell(binding);
		Object value = shell.evaluate(mapperScript);

		if (value instanceof MappingResult) {
			return (MappingResult) value;
		} else {
			log.error("The mapper script did not return the expected {" + MappingResult.class.getName() + "}");
			throw new RuntimeException("Mapper script failed for {" + uuid + "}");
		}
	}

	@Override
	public boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		log.info("Checking token. Accepting..");
		printToken(token);
		return true;
	}

	private void printToken(JsonObject token) {
		String username = token.getString("preferred_username");
		System.out.println("Token for {" + username + "}");
		System.out.println(token.encodePrettily());
	}

	/**
	 * Load the public keys for the configured realm from keycloak.
	 */
	private void loadPublicKeys() {
		KeycloakPluginConfig config = getConfig();

		publicKeys = Collections.emptySet();

		if (!config.isLoadPublicKeys()) {
			return;
		}

		try {
			URL url = new URL(config.getUrl());
			String proto = url.getProtocol();
			String host = url.getHost();
			int port = url.getPort();

			if (port == -1) {
				port = proto.equalsIgnoreCase("https") ? 443 : 80;
			}

			String realmName = config.getRealmName();
			Set<JsonObject> keys = KeycloakUtils.loadJWKs(proto, host, port, realmName);

			if (log.isDebugEnabled()) {
				for (JsonObject key : keys) {
					log.debug("Loaded public key\n{}\n", key.encodePrettily());
				}
			}

			publicKeys = keys;
		} catch (Exception e) {
			log.error("Error while loading public keys: {}", e.getMessage());

			if (log.isTraceEnabled()) {
				log.error(e);
			}
		}
	}

	private void loadMapperScript() throws IOException {
		File mapperScriptFile = new File(getStorageDir(), MAPPER_SCRIPT_NAME);

		if (mapperScriptFile.exists()) {
			if (log.isDebugEnabled()) {
				log.debug("Loading mapper script from {}", mapperScriptFile.getPath());
			}

			mapperScript = FileUtils.readFileToString(mapperScriptFile);
		} else {
			log.debug("Using default mapper script");

			InputStream defaultScript = getClass().getResourceAsStream("/defaultMapperScript.groovy");

			mapperScript = IOUtils.toString(defaultScript, StandardCharsets.UTF_8);
		}
	}
}
