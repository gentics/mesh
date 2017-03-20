package com.gentics.mesh.graphql;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static graphql.GraphQL.newGraphQL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphql.type.QueryTypeProvider;
import com.gentics.mesh.json.JsonUtil;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class GraphQLHandler {

	private static final Logger log = LoggerFactory.getLogger(GraphQLHandler.class);

	@Inject
	public QueryTypeProvider typeProvider;

	@Inject
	public Database db;

	@Inject
	public GraphQLHandler() {
	}

	/**
	 * Handle the GraphQL query
	 * 
	 * @param ac
	 * @param body
	 *            GraphQL query
	 */
	public void handleQuery(InternalActionContext ac, String body) {
		try (NoTx noTx = db.noTx()) {
			JsonObject queryJson = new JsonObject(body);
			String query = queryJson.getString("query");
			GraphQL graphQL = newGraphQL(typeProvider.getRootSchema(ac.getProject())).build();
			ExecutionResult result = graphQL.execute(query, ac);
			List<GraphQLError> errors = result.getErrors();
			if (!errors.isEmpty()) {
				log.error("Could not execute query {" + query + "}");
				for (GraphQLError error : errors) {
					if (error.getLocations() == null || error.getLocations()
							.isEmpty()) {
						log.error(error.getErrorType() + " " + error.getMessage());
					} else {
						for (SourceLocation location : error.getLocations()) {
							log.error(error.getErrorType() + " " + error.getMessage() + " " + location.getColumn() + ":" + location.getLine());
						}
					}
				}
				ac.fail(error(BAD_REQUEST, "graphql_error_while_executing"));
			} else {
				Map<String, Object> data = (Map<String, Object>) result.getData();
				JsonObject response = new JsonObject();
				response.put("data", new JsonObject(JsonUtil.toJson(data)));
				// rc.response().putHeader("Content-Type", "application/json");
				// rc.response().end(response.toString());
				ac.send(response.toString(), OK);
			}
		}

	}
}
