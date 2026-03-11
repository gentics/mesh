package com.gentics.mesh.rest.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.raml.model.MimeType;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.InternalEndpointRoute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.PlatformHandler;

/**
 * @see InternalEndpointRoute
 */
public class InternalEndpointRouteImpl extends com.gentics.vertx.openapi.metadata.InternalEndpointRouteImpl implements InternalEndpointRoute {

	protected Set<MeshEvent> events = new HashSet<>();

	/**
	 * Create a new endpoint wrapper using the provided router to create the wrapped
	 * route instance.
	 *
	 * @param router
	 * @param localConfigApi
	 * @param db
	 */
	public InternalEndpointRouteImpl(Router router, LocalConfigApi localConfigApi, Database db) {
		super(router);
		setInsecure(false);
		ReadOnlyHandler readOnlyHandler = new ReadOnlyHandler(localConfigApi, db);
		route.handler(readOnlyHandler);
	}

	@Override
	@Deprecated
	public InternalEndpointRoute blockingHandler(Handler<RoutingContext> requestHandler) {
		super.blockingHandler(requestHandler);
		return this;
	}

	@Override
	protected String getJsonSchema(JsonSchema schema) {
		return JsonUtil.getJsonSchema(schema);
	}

	@Override
	protected JsonSchema getJsonSchemaObject(Class<?> clazz) {
		return JsonUtil.getJsonSchemaObject(clazz);
	}

	@Override
	public InternalEndpointRoute events(MeshEvent... events) {
		this.events.addAll(Arrays.asList(events));
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(JSONObject jsonObject) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		String json = jsonObject.toString();
		mimeType.setExample(json);
		bodyMap.put("application/json", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute setInsecure(boolean insecure) {
		if (insecure) {
			setSecuritySchemes(Collections.emptyList());
		} else {
			setSecuritySchemes(Collections.singletonList("bearerAuth"));
		}
		return this;
	}

	private class ReadOnlyHandler implements PlatformHandler {

		private final LocalConfigApi localConfigApi;
		private final Database db;

		public ReadOnlyHandler(LocalConfigApi localConfigApi, Database db) {
			this.localConfigApi = localConfigApi;
			this.db = db;
		}

		@Override
		public void handle(RoutingContext rc) {
			if (!isMutating()) {
				rc.next();
			} else {
				if (db.isReadOnly(true)) {
					rc.fail(error(HttpResponseStatus.METHOD_NOT_ALLOWED, "error_readonly_mode"));
				}
				localConfigApi.getActiveConfig().subscribe(config -> {
					if (config.isReadOnly()) {
						rc.fail(error(HttpResponseStatus.METHOD_NOT_ALLOWED, "error_readonly_mode"));
					} else {
						rc.next();
					}
				});
			}
		}
	}
}
