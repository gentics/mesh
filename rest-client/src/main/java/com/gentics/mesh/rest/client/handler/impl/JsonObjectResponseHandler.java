package com.gentics.mesh.rest.client.handler.impl;

import com.gentics.mesh.rest.client.handler.AbstractResponseHandler;
import com.gentics.mesh.rest.client.handler.GenericMessageErrorHandler;
import com.gentics.mesh.rest.client.handler.JsonObjectSuccessHandler;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

/**
 * The mesh response handler which will return JSON using the {@link JsonObject} class.
 */
public class JsonObjectResponseHandler extends AbstractResponseHandler<JsonObject> implements JsonObjectSuccessHandler, GenericMessageErrorHandler<JsonObject> {

	public JsonObjectResponseHandler(HttpMethod method, String uri) {
		super(method, uri);
	}

}