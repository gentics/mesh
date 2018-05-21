package com.gentics.mesh.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.OAuth2Options;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

@Singleton
@SuppressWarnings("restriction")
public class MeshOAuthServiceImpl implements MeshOAuthService {

	private OAuth2AuthHandler oauth2Handler;
	private NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
	private OAuth2Options options;
	private String mapperScript = null;
	private OAuth2Auth oauth2Provider;

	@Inject
	public MeshOAuthServiceImpl(@Named("oauth") JsonObject config) {
		Vertx vertx = Mesh.vertx();
		this.options = Mesh.mesh().getOptions().getAuthenticationOptions().getOauth2();
		this.mapperScript = loadScript();

		this.oauth2Provider = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);

		OAuth2Auth keyCloakAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		// TODO configure callback url
		this.oauth2Handler = OAuth2AuthHandler.create(keyCloakAuth, "http://localhost:8080");
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
		File scriptFile = new File(path);
		if (scriptFile.exists()) {
			try {
				return FileUtils.readFileToString(scriptFile, Charset.defaultCharset());
			} catch (IOException e) {
				throw error(INTERNAL_SERVER_ERROR, "oauth_mapper_file_not_readable", e);
			}
		} else {
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
	public void handle(RoutingContext rc) {
		oauth2Handler.handle(rc);
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
}
