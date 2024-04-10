package com.gentics.mesh.plugin;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.*;

/**
 * Wrapper for the regular Vert.x routing context.
 */
public class PluginContext implements RoutingContext {

	private static final Pattern BEARER = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

	private final RoutingContext rc;

	private final PluginEnvironment env;

	/**
	 * Create a new plugin context which will wrap the {@link RoutingContext} of the handled request.
	 * 
	 * @param rc
	 * @param env
	 */
	public PluginContext(RoutingContext rc, PluginEnvironment env) {
		this.rc = rc;
		this.env = env;
	}

	/**
	 * Return a mesh client which will use the same authenticated user as the inbound request that is being handled.
	 * 
	 * @return
	 */
	public MeshRestClient client() {
		String token = parseHeader(rc);
		// The authentication token / header may be missing if the inbound request was anonymous.
		return env.createClient(token);
	}

	/**
	 * Extract the token value from the header.
	 * 
	 * @param rc
	 * @return Token value or null if no token could be found
	 */
	private String parseHeader(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		final String authorization = request.headers().get(AUTHORIZATION);
		if (authorization != null) {
			String[] parts = authorization.split(" ");
			if (parts.length == 2) {
				final String scheme = parts[0], credentials = parts[1];
				if (BEARER.matcher(scheme).matches()) {
					return credentials;
				}
			}
		}
		return null;
	}

	/**
	 * Return the current project info.
	 * 
	 * @return Project info
	 */
	public JsonObject project() {
		return (JsonObject) rc.data().get("mesh.project");
	}

	/**
	 * Return the name of the current project.
	 * 
	 * @return Project name
	 */
	public String projectName() {
		return project().getString("name");
	}

	@Override
	public HttpServerRequest request() {
		return rc.request();
	}

	@Override
	public HttpServerResponse response() {
		return rc.response();
	}

	@Override
	public void next() {
		rc.next();
	}

	@Override
	public void fail(int statusCode) {
		rc.fail(statusCode);
	}

	@Override
	public void fail(Throwable throwable) {
		rc.fail(throwable);
	}

	/**
	 * End the request using the model as a response.
	 * 
	 * @param restModel
	 *            REST Model of the response
	 * @param status
	 *            Status of the response
	 */
	public void send(RestModel restModel, int status) {
		send(restModel.toJson(env.options().getHttpServerOptions().isMinifyJson()), status, HttpConstants.APPLICATION_JSON);
	}

	/**
	 * End the request using the provided response.
	 * 
	 * @param body
	 *            Content to be send
	 * @param status
	 *            Status of the response
	 * @param contentType
	 *            Content type of the response
	 */
	public void send(String body, int status, String contentType) {
		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
		rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
		rc.response().setStatusCode(status).end(body);
	}

	/**
	 * End the request without a body and just the status code.
	 * 
	 * @param status
	 *            Status of the response
	 */
	public void send(int status) {
		rc.response().setStatusCode(status).end();
	}

	@Override
	public RoutingContext put(String key, Object obj) {
		return rc.put(key, obj);
	}

	@Override
	public <T> T get(String key) {
		return rc.get(key);
	}

	@Override
	public <T> T get(String s, T t) {
		return rc.get(s, t);
	}

	@Override
	public <T> T remove(String key) {
		return rc.remove(key);
	}

	@Override
	public Map<String, Object> data() {
		return rc.data();
	}

	@Override
	public Vertx vertx() {
		return rc.vertx();
	}

	@Override
	public @Nullable String mountPoint() {
		return rc.mountPoint();
	}

	@Override
	public Route currentRoute() {
		return rc.currentRoute();
	}

	@Override
	public String normalisedPath() {
		return rc.normalisedPath();
	}

	@Override
	public String normalizedPath() {
		return rc.normalizedPath();
	}

	@Override
	public @Nullable Cookie getCookie(String name) {
		return rc.getCookie(name);
	}

	@Override
	public @Nullable Cookie removeCookie(String name, boolean invalidate) {
		return rc.removeCookie(name, invalidate);
	}

	@Override
	public int cookieCount() {
		return rc.cookieCount();
	}

	@Override
	public @Nullable String getBodyAsString() {
		return rc.getBodyAsString();
	}

	@Override
	public @Nullable String getBodyAsString(String encoding) {
		return rc.getBodyAsString(encoding);
	}

	@Override
	public @io.vertx.codegen.annotations.Nullable JsonObject getBodyAsJson(int i) {
		return rc.getBodyAsJson(i);
	}

	@Override
	public @io.vertx.codegen.annotations.Nullable JsonArray getBodyAsJsonArray(int i) {
		return rc.getBodyAsJsonArray(i);
	}

	@Override
	public @Nullable JsonObject getBodyAsJson() {
		return rc.getBodyAsJson();
	}

	@Override
	public @Nullable JsonArray getBodyAsJsonArray() {
		return rc.getBodyAsJsonArray();
	}

	@Override
	public @Nullable Buffer getBody() {
		return rc.getBody();
	}

	@Override
	public RequestBody body() {
		return rc.body();
	}

	@Override
	public List<FileUpload> fileUploads() {
		return rc.fileUploads();
	}

	@Override
	public @Nullable Session session() {
		return rc.session();
	}

	@Override
	public @Nullable User user() {
		return rc.user();
	}

	@Override
	public @Nullable Throwable failure() {
		return rc.failure();
	}

	@Override
	public int statusCode() {
		return rc.statusCode();
	}

	@Override
	public @Nullable String getAcceptableContentType() {
		return rc.getAcceptableContentType();
	}

	@Override
	public ParsedHeaderValues parsedHeaders() {
		return rc.parsedHeaders();
	}

	@Override
	public int addHeadersEndHandler(Handler<Void> handler) {
		return rc.addHeadersEndHandler(handler);
	}

	@Override
	public boolean removeHeadersEndHandler(int handlerID) {
		return rc.removeHeadersEndHandler(handlerID);
	}

	@Override
	public int addBodyEndHandler(Handler<Void> handler) {
		return rc.addBodyEndHandler(handler);
	}

	@Override
	public boolean removeBodyEndHandler(int handlerID) {
		return rc.removeBodyEndHandler(handlerID);
	}

	@Override
	public boolean failed() {
		return rc.failed();
	}

	@Override
	public void setBody(Buffer body) {
		rc.setBody(body);
	}

	@Override
	public void setSession(Session session) {
		rc.setSession(session);
	}

	@Override
	public void setUser(User user) {
		rc.setUser(user);
	}

	@Override
	public void clearUser() {
		rc.clearUser();
	}

	@Override
	public void setAcceptableContentType(@Nullable String contentType) {
		rc.setAcceptableContentType(contentType);
	}

	@Override
	public void reroute(HttpMethod method, String path) {
		rc.reroute(method, path);
	}

	@Override
	public Map<String, String> pathParams() {
		return rc.pathParams();
	}

	@Override
	public @Nullable String pathParam(String name) {
		return rc.pathParam(name);
	}

	@Override
	public MultiMap queryParams() {
		return rc.queryParams();
	}

	@Override
	public MultiMap queryParams(Charset charset) {
		return rc.queryParams(charset);
	}

	@Override
	public @Nullable List<String> queryParam(String query) {
		return rc.queryParam(query);
	}

	@Override
	public void fail(int statusCode, Throwable throwable) {
		rc.fail(statusCode, throwable);
	}

	@Override
	public RoutingContext addCookie(io.vertx.core.http.Cookie cookie) {
		return rc.addCookie(cookie);
	}

	@Override
	public Map<String, io.vertx.core.http.Cookie> cookieMap() {
		return rc.cookieMap();
	}

	@Override
	public int addEndHandler(Handler<AsyncResult<Void>> handler) {
		return rc.addEndHandler(handler);
	}

	@Override
	public boolean removeEndHandler(int handlerID) {
		return rc.removeEndHandler(handlerID);
	}

	@Override
	public boolean isSessionAccessed() {
		return rc.isSessionAccessed();
	}
}
