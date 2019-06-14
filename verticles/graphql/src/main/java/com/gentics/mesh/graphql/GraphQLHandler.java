package com.gentics.mesh.graphql;

import static graphql.GraphQL.newGraphQL;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.rest.error.AbstractUnavailableException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.QueryTypeProvider;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
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
	 * Handle the GraphQL query.
	 *
	 * @param gc
	 *            Context
	 * @param body
	 *            GraphQL query
	 */
	public void handleQuery(GraphQLContext gc, String body) {
		try (Tx tx = db.tx()) {
			JsonObject queryJson = new JsonObject(body);
			String query = queryJson.getString("query");
			GraphQL graphQL = newGraphQL(typeProvider.getRootSchema(gc)).build();
			ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).context(gc).variables(extractVariables(queryJson))
					.build();
			ExecutionResult result = graphQL.execute(executionInput);
			List<GraphQLError> errors = result.getErrors();
			JsonObject response = new JsonObject();
			if (!errors.isEmpty()) {
				addErrors(errors, response);
				log.warn("Encountered {" + errors.size() + "} errors while executing query {" + query + "}");
				if (log.isDebugEnabled()) {
					for (GraphQLError error : errors) {
						String loc = "unknown location";
						if (error.getLocations() != null) {
							loc = error.getLocations().stream().map(Object::toString).collect(Collectors.joining(","));
						}
						log.debug("Error: " + error.getErrorType() + ":" + error.getMessage() + ":" + loc);
					}
				}
			}
			if (result.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) result.getData();
				response.put("data", new JsonObject(data));
			}
			gc.send(response.encodePrettily(), OK);
		}

	}

	/**
	 * Extracts the variables of a query as a map. Returns empty map if no variables are found.
	 *
	 * @param request
	 *            The request body
	 * @return GraphQL variables
	 */
	private Map<String, Object> extractVariables(JsonObject request) {
		JsonObject variables = request.getJsonObject("variables");
		if (variables == null) {
			return Collections.emptyMap();
		} else {
			return variables.getMap();
		}
	}

	/**
	 * Add the listed errors to the response.
	 * 
	 * @param errors
	 * @param response
	 */
	private void addErrors(List<GraphQLError> errors, JsonObject response) {
		JsonArray jsonErrors = new JsonArray();
		response.put("errors", jsonErrors);
		for (GraphQLError error : errors) {
			JsonObject jsonError = new JsonObject();
			if (error instanceof ExceptionWhileDataFetching) {
				ExceptionWhileDataFetching dataError = (ExceptionWhileDataFetching) error;
				if (dataError.getException() instanceof AbstractUnavailableException) {
					AbstractUnavailableException restException = (AbstractUnavailableException) dataError.getException();
					// TODO translate error
					// TODO add i18n parameters
					jsonError.put("message", restException.getI18nKey());
					jsonError.put("type", restException.getType());
					jsonError.put("elementId", restException.getElementId());
					jsonError.put("elementType", restException.getElementType());
				} else {
					log.error("Error while fetching data.", dataError.getException());
					jsonError.put("message", dataError.getMessage());
					jsonError.put("type", dataError.getErrorType());
				}
			} else {
				jsonError.put("message", error.getMessage());
				jsonError.put("type", error.getErrorType());
				if (error.getLocations() != null && !error.getLocations().isEmpty()) {
					JsonArray errorLocations = new JsonArray();
					jsonError.put("locations", errorLocations);
					for (SourceLocation location : error.getLocations()) {
						JsonObject errorLocation = new JsonObject();
						errorLocation.put("line", location.getLine());
						errorLocation.put("column", location.getColumn());
						errorLocations.add(errorLocation);
					}
				}
			}
			jsonErrors.add(jsonError);
		}
	}
}
