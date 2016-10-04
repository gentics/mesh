package com.gentics.mesh.graphql;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphql.type.RootTypeProvider;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

import graphql.GraphQL;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.StaticHandler;

@Singleton
public class GraphQLVerticle extends AbstractWebVerticle {

	public GraphQLVerticle() {
		super("graphql", null);
	}

	@Inject
	public RootTypeProvider typeProvider;

	@Inject
	public GraphQLVerticle(RouterStorage routerStorage) {
		super("graphql", routerStorage);
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		Endpoint queryEndpoint = createEndpoint();
		queryEndpoint.method(POST);
		queryEndpoint.description("Endpoint which accepts GraphQL queries.");
		queryEndpoint.path("/");
		queryEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String body = ac.getBodyAsString();
			TestNode context = new TestNode();
			context.setUuid(UUIDUtil.randomUUID());
			context.getFields().add(new DateTestField("dateFieldName", "todayValue"));
			context.getFields().add(new StringTestField("stringFieldName", true));
			JsonObject queryJson = new JsonObject(body);
			String query = queryJson.getString("query");
			// System.out.println(query);
			Map<String, Object> result = (Map<String, Object>) new GraphQL(typeProvider.getRootSchema()).execute(query, ac).getData();
			if (result == null) {
				rc.response().setStatusCode(400);
				rc.response().end("Query could not be executed");
			} else {
				rc.response().putHeader("Content-Type", "application/json");
				JsonObject response = new JsonObject();
				response.put("data", new JsonObject(JsonUtil.toJson(result)));
				rc.response().end(response.toString());
			}
		});

		StaticHandler staticHandler = StaticHandler.create("graphiql");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		route("/browser/*").method(GET).handler(staticHandler);

		// Redirect handler
		route("/browser").method(GET).handler(rc -> {
			if ("/browser".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", rc.request().path() + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});

	}

	@Override
	public String getDescription() {
		return "Graph QL endpoint";
	}

}
