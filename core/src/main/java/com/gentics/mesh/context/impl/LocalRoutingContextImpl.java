package com.gentics.mesh.context.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.core.data.MeshAuthUser;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class LocalRoutingContextImpl<T> implements RoutingContext {

	private Promise<T> promise = Promise.promise();

	private HttpServerResponse responseMock = null;

	private User user;

	public LocalRoutingContextImpl(MeshAuthUser user) {
		this.user = user;
	}

	public Future<T> getFuture() {
		return promise.future();
	}

	@Override
	public HttpServerRequest request() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse response() {
		return responseMock;
	}

	@Override
	public void next() {
		// TODO Auto-generated method stub

	}

	@Override
	public void fail(int statusCode) {
		promise.fail("Fail with code " + statusCode);
	}

	@Override
	public void fail(Throwable throwable) {
		promise.fail(throwable);
	}

	@Override
	public void fail(int statusCode, Throwable throwable) {
		// TODO Auto-generated method stub

	}

	@Override
	public RoutingContext put(String key, Object obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T get(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T remove(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> data() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vertx vertx() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String mountPoint() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Route currentRoute() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String normalisedPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable Cookie getCookie(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoutingContext addCookie(Cookie cookie) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable Cookie removeCookie(String name, boolean invalidate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int cookieCount() {
		return 0;
	}

	@Override
	public Set<Cookie> cookies() {
		return Collections.emptySet();
	}

	@Override
	public @Nullable String getBodyAsString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String getBodyAsString(String encoding) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable JsonObject getBodyAsJson() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable JsonArray getBodyAsJsonArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable Buffer getBody() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<FileUpload> fileUploads() {
		return Collections.emptySet();
	}

	@Override
	public @Nullable Session session() {
		return null;
	}

	@Override
	public @Nullable User user() {
		return user;
	}

	@Override
	public @Nullable Throwable failure() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int statusCode() {
		return 0;
	}

	@Override
	public @Nullable String getAcceptableContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ParsedHeaderValues parsedHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int addHeadersEndHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean removeHeadersEndHandler(int handlerID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int addBodyEndHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean removeBodyEndHandler(int handlerID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean failed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setBody(Buffer body) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSession(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public void clearUser() {
		this.user = null;
	}

	@Override
	public void setAcceptableContentType(@Nullable String contentType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reroute(HttpMethod method, String path) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<Locale> acceptableLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> pathParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String pathParam(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultiMap queryParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> queryParam(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
