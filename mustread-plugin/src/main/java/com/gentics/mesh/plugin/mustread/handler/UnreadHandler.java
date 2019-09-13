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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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

		String mustReadQuery = "";
		String tmpDataQuery = "";
		String alreadyReadQuery;

		try {
			String mustReadFile = getPlugin().getGraphQlDir() + File.separator + getProjectConfig().getMustReadGraphQlFile();
			String dataFile = getPlugin().getGraphQlDir() + File.separator + getProjectConfig().getDataGraphQlFile();

			mustReadQuery = IOUtils.toString(new FileInputStream(mustReadFile));
			tmpDataQuery = IOUtils.toString(new FileInputStream(dataFile));
			alreadyReadQuery = IOUtils.toString(getPlugin().getClass().getResourceAsStream("/graphql/AlreadyRead.graphql"));
		} catch (Exception e) {
			log.error("Error while reading gql files", e);
			doHttpCode(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return;
		}
		MeshRestClient client = getAdminClient();
		UUIDs mustReadUuids = new UUIDs();
		final String dataQuery = tmpDataQuery;

		client.graphqlQuery(getProjectName(), mustReadQuery).toSingle()
			.flatMap(mustReadResponse -> {
				GraphQLRequest request = new GraphQLRequest();
				JsonObject variables = new JsonObject();

				JsonHelper.fetchKeys(mustReadResponse, getProjectConfig().getUuidFieldName(), mustReadUuids::addUuid);

				variables.put("path", getNodeController().generateUserReadFolder(getProjectName(), getUser()));

				request.setQuery(alreadyReadQuery);
				request.setVariables(variables);

				return client.graphql(getProjectName(), request).toSingle();
			})
			.flatMap(alreadyReadResponse -> {
				UUIDs alreadyReadUuids = new UUIDs();

				JsonHelper.fetchKeys(alreadyReadResponse, "node_id", alreadyReadUuids::addUuid);

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
