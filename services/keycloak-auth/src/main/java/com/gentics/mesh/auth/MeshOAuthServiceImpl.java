package com.gentics.mesh.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2Options;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
@SuppressWarnings("restriction")
public class MeshOAuthServiceImpl implements MeshOAuthService {

	private static final Logger log = LoggerFactory.getLogger(MeshOAuthServiceImpl.class);

	private OAuth2AuthHandler oauth2Handler;
	private NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
	private OAuth2Options options;
	private String mapperScript = null;
	private OAuth2Auth oauth2Provider;
	private OAuth2AuthCookieHandler oauthCookieHandler;

	@Inject
	public MeshOAuthServiceImpl() {
		Vertx vertx = Mesh.vertx();
		this.options = Mesh.mesh().getOptions().getAuthenticationOptions().getOauth2();
		if (options == null || !options.isEnabled()) {
			return;
		}

		JsonObject config = loadRealmInfo(vertx, Mesh.mesh().getOptions());
		this.mapperScript = loadScript();

		this.oauth2Provider = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		this.oauthCookieHandler = new OAuth2AuthCookieHandlerImpl(oauth2Provider);

		// TODO configure callback url
		this.oauth2Handler = OAuth2AuthHandler.create(oauth2Provider, "http://localhost:8080");
		// Don't setup the callback mechanism. This would create redirects in our rest api which we don't want. Instead we want to handle auth errors with http
		// error codes.
		// oauth2.setupCallback(router.get("/callback"));

	}

	/**
	 * Ensure that the user principle information is in sync with the graph database.
	 */
	private void syncUserWithGraph() {
		// TODO sync user
		// TODO sync roles - Add script to handle mapped fields
		// TODO sync groups - Add script to handle mapped fields
	}

	private String loadScript() {
		String path = options.getMapperScriptPath();
		if (path == null) {
			return null;
		}
		File scriptFile = new File(path);
		if (scriptFile.exists()) {
			try {
				return FileUtils.readFileToString(scriptFile, Charset.defaultCharset());
			} catch (IOException e) {
				throw error(INTERNAL_SERVER_ERROR, "oauth_mapper_file_not_readable", e);
			}
		} else {
			log.warn("The OAuth2 mapper script {" + path + "} could not be found.");
			return null;
		}
	}

	private void executeMapperScript() throws ScriptException {
		ScriptEngine engine = factory.getScriptEngine(new Sandbox());
		// engine.put("input", nodeJson);
		engine.eval(mapperScript);
		Object transformedModel = engine.get("output");
	}

	@Override
	public void secure(Route route) {
		route.handler(oauthCookieHandler);
		route.handler(oauth2Handler).failureHandler(rc -> {
			if (rc.failed()) {
				Throwable error = rc.failure();
				if (error instanceof NoStackTraceThrowable) {
					NoStackTraceThrowable s = (NoStackTraceThrowable) error;
					String msg = s.getMessage();
					if ("callback route is not configured.".equalsIgnoreCase(msg)) {
						// Suppress the error and use 401 instead
						rc.response().setStatusCode(401).end();
						return;
					}
				} else {
					rc.fail(error);
				}
			}
		});

	}

	public OAuth2Auth getOauth2Provider() {
		return oauth2Provider;
	}

	/**
	 * Sandbox classfilter that filters all classes
	 */
	protected static class Sandbox implements ClassFilter {
		@Override
		public boolean exposeToScripts(String className) {
			return false;
		}
	}

	/**
	 * Load the settings and enhance them with the public realm information from the auth server.
	 * 
	 * @return
	 */
	public JsonObject loadRealmInfo(Vertx vertx, MeshOptions options) {
		if (options == null) {
			log.debug("Mesh options not specified. Can't setup OAuth2.");
			return null;
		}
		AuthenticationOptions authOptions = options.getAuthenticationOptions();
		if (authOptions == null) {
			log.debug("Mesh auth options not specified. Can't setup OAuth2.");
			return null;
		}
		JsonObject config = options.getAuthenticationOptions().getOauth2().getConfig();
		if (config == null) {
			log.debug("OAuth config  not specified. Can't setup OAuth2.");
			return null;
		}

		String realmName = config.getString("realm");
		Objects.requireNonNull(realmName, "The realm property was not found in the oauth2 config");
		String url = config.getString("auth-server-url");
		Objects.requireNonNull(realmName, "The auth-server-url property was not found in the oauth2 config");

		try {
			URL authServerUrl = new URL(url);
			String authServerHost = authServerUrl.getHost();
			config.put("auth-server-host", authServerHost);
			int authServerPort = authServerUrl.getPort();
			config.put("auth-server-port", authServerPort);
			String authServerProtocol = authServerUrl.getProtocol();
			config.put("auth-server-protocol", authServerProtocol);

			JsonObject json = fetchPublicRealmInfo(authServerProtocol, authServerHost, authServerPort, realmName);
			config.put("auth-server-url", authServerProtocol + "://" + authServerHost + ":" + authServerPort + "/auth");
			config.put("realm-public-key", json.getString("public_key"));
			return config;
		} catch (Exception e) {
			throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "oauth_config_error", e);
		}

	}

	private JsonObject fetchPublicRealmInfo(String protocol, String host, int port, String realmName) throws IOException {
		Builder builder = new OkHttpClient.Builder();
		OkHttpClient client = builder.build();

		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(protocol + "://" + host + ":" + port + "" + "/auth/realms/" + realmName)
			.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			log.error(response.body().toString());
			throw new RuntimeException("Error while loading realm info. Got code {" + response.code() + "}");
		}
		return new JsonObject(response.body().string());

	}
}
