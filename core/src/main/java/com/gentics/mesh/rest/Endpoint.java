package com.gentics.mesh.rest;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Endpoint implements Route {

	private Route route;

	private String displayName;

	private String description;

	private Map<Integer, RestModel> exampleResponses = new HashMap<>();

	private String[] traits = new String[] {};

	private RestModel exampleRequest = null;

	private String pathRegex;

	public Endpoint(Router router) {
		this.route = router.route();
	}

	public Route path(String path) {
		return route.path(path);
	}

	@Override
	public Route method(HttpMethod method) {
		return route.method(method);
	}

	@Override
	public Route pathRegex(String path) {
		this.pathRegex = path;
		return route.pathRegex(path);
	}

	@Override
	public Route produces(String contentType) {
		return route.produces(contentType);
	}

	@Override
	public Route consumes(String contentType) {
		return route.consumes(contentType);
	}

	@Override
	public Route order(int order) {
		return route.order(order);
	}

	@Override
	public Route last() {
		return route.last();
	}

	@Override
	public Route handler(Handler<RoutingContext> requestHandler) {
		return route.handler(requestHandler);
	}

	@Override
	public Route blockingHandler(Handler<RoutingContext> requestHandler) {
		return route.blockingHandler(requestHandler);
	}

	@Override
	public Route blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) {
		return route.blockingHandler(requestHandler, ordered);
	}

	@Override
	public Route failureHandler(Handler<RoutingContext> failureHandler) {
		return route.failureHandler(failureHandler);
	}

	@Override
	public Route remove() {
		return route.remove();
	}

	@Override
	public Route disable() {
		return route.disable();
	}

	@Override
	public Route enable() {
		return route.enable();
	}

	@Override
	public Route useNormalisedPath(boolean useNormalisedPath) {
		return route.useNormalisedPath(useNormalisedPath);
	}

	@Override
	public @Nullable String getPath() {
		return route.getPath();
	}

	public Endpoint displayName(String name) {
		this.displayName = name;
		return this;
	}

	public Endpoint description(String description) {
		this.description = description;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Endpoint exampleResponse(int code, RestModel model) {
		exampleResponses.put(code, model);
		return this;
	}

	public Endpoint exampleRequest(RestModel model) {
		this.exampleRequest = model;
		return this;
	}

	/**
	 * Set the traits information.
	 * 
	 * @param traits
	 * @return
	 */
	public Endpoint traits(String... traits) {
		this.traits = traits;
		return this;
	}

	/**
	 * Return the traits which were set for this endpoint.
	 * 
	 * @return
	 */
	public String[] getTraits() {
		return traits;
	}

	public Map<Integer, RestModel> getExampleResponses() {
		return exampleResponses;
	}

	public RestModel getExampleRequest() {
		return exampleRequest;
	}

	public String getPathRegex() {
		return pathRegex;
	}

}
