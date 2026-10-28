package com.gentics.mesh.plugin.mustread.handler;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.controller.NodeController;
import com.gentics.mesh.plugin.mustread.helper.JsonHelper;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.plugin.mustread.object.UUIDs;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;

public class UnreadHandler extends BaseHandler {

	private static final Logger log = LoggerFactory.getLogger(UnreadHandler.class);

	private NodeController nodeController;

	public UnreadHandler(MustReadPlugin plugin, MustReadConfig config, LogHelper logHelper, NodeController nodeController) {
		super(plugin, config, logHelper);

		this.nodeController = nodeController;
	}

	@Override
	public void handle(RoutingContext routingContext) {
		initialize(routingContext);

		if (!checkProjectConfiguration()) {
			doHttpCode(HttpResponseStatus.NOT_IMPLEMENTED);
			return;
		}

		String uuidQuery = "";
		String tmpDataQuery = "";
		String alreadyReadQuery;

		try {
			String uuidFile = getPlugin().getGraphQlDir() + File.separator + getProjectConfig().getQueryFiles().getUuid();
			String dataFile = getPlugin().getGraphQlDir() + File.separator + getProjectConfig().getQueryFiles().getData();

			uuidQuery = IOUtils.toString(new FileInputStream(uuidFile));
			tmpDataQuery = IOUtils.toString(new FileInputStream(dataFile));
			alreadyReadQuery = IOUtils.toString(getPlugin().getClass().getResourceAsStream("/graphql/AlreadyRead.graphql"));
		} catch (Exception e) {
			log.error("Error while reading graphQL files", e);
			doHttpCode(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return;
		}
		MeshRestClient client = getAdminClient();
		final UUIDs mustReadUuids = new UUIDs();
		// dataQuery must be final so we can use it inside of lambdas
		final String dataQuery = tmpDataQuery;

		client.graphqlQuery(getProjectName(), uuidQuery).toSingle()
			.flatMap(mustReadResponse -> {
				GraphQLRequest request = new GraphQLRequest();
				JsonObject variables = new JsonObject();

				// We fetch all uuids regardless of where they are inside of the returned grapql
				JsonHelper.fetchKeys(mustReadResponse, getProjectConfig().getFieldNames().getUuid(), getProjectConfig().isPublishedOnly(), getProjectConfig().getFieldNames().getPublished(), mustReadUuids::addUuid);

				variables.put("path", getNodeController().generateUserReadFolder(getProjectName(), getUser()));

				request.setQuery(alreadyReadQuery);
				request.setVariables(variables);

				return client.graphql(getProjectName(), request).toSingle();
			})
			.flatMap(alreadyReadResponse -> {
				UUIDs alreadyReadUuids = new UUIDs();

				JsonHelper.fetchKeys(alreadyReadResponse, "node_id", false, "", alreadyReadUuids::addUuid);

				// We remove all that are already read
				mustReadUuids.removeAll(alreadyReadUuids);

				GraphQLRequest request = new GraphQLRequest();
				JsonObject variables = new JsonObject();

				variables.put("uuids", new JsonArray(mustReadUuids.getUUIDs()));

				request.setQuery(dataQuery);
				request.setVariables(variables);

				return client.graphql(getProjectName(), request).toSingle();
			})
			.subscribe(dataResponse -> {
				doJsonResponse(dataResponse.getData());
			}, error -> {
				log.error(getLogHelper().generateLogMessage(getProjectName(), "Error during data fetch"), error);
				doHttpCode(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			});
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	private NodeController getNodeController() {
		return nodeController;
	}
}
