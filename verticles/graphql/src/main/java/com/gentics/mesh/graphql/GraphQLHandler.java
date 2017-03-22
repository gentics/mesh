package com.gentics.mesh.graphql;

import static graphql.GraphQL.newGraphQL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphql.type.QueryTypeProvider;
import com.gentics.mesh.json.JsonUtil;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
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
			JsonObject response = new JsonObject();
			if (!errors.isEmpty()) {
				log.error("Could not execute query {" + query + "}");
			}

			addErrors(errors, response);

			if (result.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) result.getData();
				response.put("data", new JsonObject(JsonUtil.toJson(data)));
			}

			boolean hasErrors = result.getErrors() != null && !result.getErrors()
					.isEmpty();
			HttpResponseStatus code = hasErrors ? BAD_REQUEST : OK;
			ac.send(response.toString(), code);
		}

	}

	/**
	 * Add the listed errors to the response.
	 * 
	 * @param errors
	 * @param response
	 */
	private void addErrors(List<GraphQLError> errors, JsonObject response) {
		if (!errors.isEmpty()) {
			JsonArray jsonErrors = new JsonArray();
			response.put("errors", jsonErrors);
			for (GraphQLError error : errors) {
				JsonObject jsonError = new JsonObject();
				if (error instanceof ExceptionWhileDataFetching) {
					ExceptionWhileDataFetching dataError = (ExceptionWhileDataFetching) error;
					if (dataError.getException() instanceof PermissionException) {
						PermissionException restException = (PermissionException) dataError.getException();
						//TODO translate error
						//TODO add i18n parameters
						jsonError.put("message", restException.getMessage());
						jsonError.put("type", restException.getType());
					} else {
						jsonError.put("message", dataError.getMessage());
						jsonError.put("type", dataError.getErrorType());
					}
				} else {
					jsonError.put("message", error.getMessage());
					jsonError.put("type", error.getErrorType());
					if (error.getLocations() != null && !error.getLocations()
							.isEmpty()) {
						JsonArray errorLocations = new JsonArray();
						jsonError.put("locations", errorLocations);
						for (SourceLocation location : error.getLocations()) {
							JsonObject errorLocation = new JsonObject();
							errorLocation.put("line", location.getLine());
							errorLocation.put("column", location.getLine());
							errorLocations.add(errorLocation);
						}
					}
				}
				jsonErrors.add(jsonError);
			}
		}
	}
}
