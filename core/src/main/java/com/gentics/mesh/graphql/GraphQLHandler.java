package com.gentics.mesh.graphql;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphql.type.RootTypeProvider;
import com.gentics.mesh.json.JsonUtil;

import graphql.GraphQL;
import io.vertx.core.json.JsonObject;

@Singleton
public class GraphQLHandler {

	@Inject
	public RootTypeProvider typeProvider;

	@Inject
	public Database db;

	@Inject
	public GraphQLHandler() {
	}

	public void handleQuery(InternalActionContext ac, String body) {
		// TestNode context = new TestNode();
		// context.setUuid(UUIDUtil.randomUUID());
		// context.getFields().add(new DateTestField("dateFieldName", "todayValue"));
		// context.getFields().add(new StringTestField("stringFieldName", true));
		try (NoTx noTx = db.noTx()) {
			JsonObject queryJson = new JsonObject(body);
			String query = queryJson.getString("query");
			// System.out.println(query);
			Map<String, Object> result = (Map<String, Object>) new GraphQL(typeProvider.getRootSchema()).execute(query, ac).getData();
			if (result == null) {
				ac.send("Query could not be executed", BAD_REQUEST);
			} else {
				JsonObject response = new JsonObject();
				response.put("data", new JsonObject(JsonUtil.toJson(result)));
				// rc.response().putHeader("Content-Type", "application/json");
				// rc.response().end(response.toString());
				ac.send(response.toString(), OK);
			}
		}

	}
}
