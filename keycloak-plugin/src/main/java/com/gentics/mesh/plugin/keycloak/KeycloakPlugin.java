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
import io.reactivex.Single;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.FileSystem;
import org.apache.commons.io.IOUtils;
import org.pf4j.PluginWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;


public class KeycloakPlugin extends AbstractCommercialPlugin<KeycloakPluginConfig> implements AuthServicePlugin {

	protected static final String DEFAULT_SCRIPT_RESOURCE = "/defaultMapperScript.groovy";

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
			// TODO Make loading of public keys non-blocking
			.andThen(Completable.fromAction(this::loadPublicKeys))
			.andThen(loadMapperScript());
	}

	@Override
	public MappingResult mapToken(HttpServerRequest httpServerRequest, String uuid, JsonObject token) {
		return evaluateMapper(httpServerRequest, uuid, token);
	}

	@Override
	public boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		log.debug("Accepting token");
		printToken(token);
		return true;
	}

	public MappingResult evaluateMapper(HttpServerRequest httpServerRequest, String uuid, JsonObject token) {
		Binding binding = new Binding();

		binding.setVariable("request", httpServerRequest);
		binding.setVariable("uuid", uuid);
		binding.setVariable("token", token);
		binding.setVariable("log", log);

		GroovyShell shell = new GroovyShell(binding);
		Object value = shell.evaluate(getMapperScript());

		if (value instanceof MappingResult) {
			return (MappingResult) value;
		} else {
			log.error("The mapper script did not return the expected {" + MappingResult.class.getName() + "}");
			throw new RuntimeException("Mapper script failed for {" + uuid + "}");
		}
	}

	public String getMapperScript() {
		return mapperScript;
	}

	private void printToken(JsonObject token) {
		if (log.isTraceEnabled()) {
			log.trace(
				"Token for user {}:\n{}",
				token.getString("preferred_username"),
				token.encodePrettily());
		}
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

	private Completable loadMapperScript() {
		File mapperScriptFile = new File(getStorageDir(), MAPPER_SCRIPT_NAME);
		FileSystem fs = rxVertx().fileSystem();

		return fs.rxReadFile(mapperScriptFile.getPath())
			.map(Buffer::toString)
			.onErrorResumeNext(this::loadDefaultMapperScript)
			.doOnSuccess(script -> mapperScript = script)
			.ignoreElement();
	}

	private Single<String> loadDefaultMapperScript(Throwable error) {
		log.error("Could not load mapper script: {}", error.getMessage());

		if (log.isTraceEnabled()) {
			log.trace(error);
		}

		log.debug("Using default mapper script");

		String script;

		try {
			script = IOUtils.toString(
				getClass().getResourceAsStream(DEFAULT_SCRIPT_RESOURCE),
				StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Could not load default mapper script: {}", e.getMessage());

			return Single.error(e);
		}

		return Single.just(script);
	}
}
