package com.gentics.mesh.plugin.mustread.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.plugin.mustread.object.User;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang.StringUtils;

import java.io.File;

abstract class BaseHandler implements Handler<RoutingContext> {

	private static final Logger log = LoggerFactory.getLogger(BaseHandler.class);

	private MustReadPlugin plugin;
	private MustReadConfig config;
	private LogHelper logHelper;

	private RoutingContext routingContext;

	BaseHandler(MustReadPlugin plugin, MustReadConfig config, LogHelper logHelper) {
		this.plugin = plugin;
		this.config = config;
		this.logHelper = logHelper;
	}

	void initialize(RoutingContext routingContext) {
		this.routingContext = routingContext;
	}

	/**
	 * Checks if all necessary configuration options for the current project have been made
	 *
	 * @return
	 */
	boolean checkProjectConfiguration() {
		ProjectConfig config = getProjectConfig();
		if (config == null) {
			getLogger().warn(getLogHelper().generateLogMessage(getProjectName(), "no configuration found!"));
			return false;
		}

		if (StringUtils.isBlank(config.getFolderName())) {
			getLogger().warn(getLogHelper().generateLogMessage(getProjectName(), "Folder to store read articles not configured"));
			return false;
		}

		String mustReadFileName = getProjectConfig().getQueryFiles().getUuid();
		String dataFileName = getProjectConfig().getQueryFiles().getData();

		if (!checkFilename(mustReadFileName, "must read query") || !checkFile(mustReadFileName)) {
			return false;
		}

		if (!checkFilename(dataFileName, "data query") || !checkFile(dataFileName)) {
			return false;
		}

		return true;
	}

	private boolean checkFilename(String fileName, String name) {
		if (StringUtils.isBlank(fileName)) {
			getLogger()
				.warn(getLogHelper().generateLogMessage(getProjectName(), String.format("the file containing the %s is not configured.", name)));
			return false;
		}

		return true;
	}

	private boolean checkFile(String fileName) {

		File file = new File(getPlugin().getGraphQlDir(), fileName);

		if (!file.exists()) {
			log.warn(getLogHelper().generateLogMessage(getProjectName(), String.format("the file '%s' does not exist.", fileName)));
			return false;
		}

		if (!file.canRead()) {
			log.warn(getLogHelper().generateLogMessage(getProjectName(), String.format("the file '%s' is not readable.", fileName)));
			return false;
		}

		return true;
	}

	void doHttpCode(HttpResponseStatus status) {
		getRoutingContext().response()
			.setStatusCode(status.code()).end(status.reasonPhrase());
	}

	void doJsonResponse(Object object, HttpResponseStatus status) {
		getRoutingContext().response()
				.setStatusCode(status.code())
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(Json.encode(object));
	}

	void doJsonResponse(String text, HttpResponseStatus status) {
		JsonObject jsonResponse = new JsonObject();

		jsonResponse.put("message", text);

		doJsonResponse(jsonResponse, status);
	}

	void doJsonResponse(String text) {
		doJsonResponse(text, HttpResponseStatus.OK);
	}

	void doJsonResponse(Object object) {
		doJsonResponse(object, HttpResponseStatus.OK);
	}


	MustReadPlugin getPlugin() {
		return plugin;
	}

	MeshRestClient getAdminClient() {
		return getPlugin().adminClient();
	}

	MustReadConfig getConfig() {
		return config;
	}

	ProjectConfig getProjectConfig() {
		return getConfig().getProjectConfiguration(getPluginContext().projectName());
	}

	/**
	 * Provides the projectname if none could be found and empty string is returned
	 *
	 * @return
	 */
	String getProjectName() {
		if (getProjectConfig() == null) {
			if (getPluginContext().project() == null) {
				return "";
			} else {
				return getPluginContext().projectName();
			}
		}

		return getProjectConfig().getName();
	}

	LogHelper getLogHelper() {
		return logHelper;
	}

	private PluginContext getPluginContext() {
		return getPlugin().wrap(getRoutingContext());
	}

	protected Logger getLogger() {
		return log;
	}

	User getUser() {
		return User.parse(getRoutingContext());
	}

	RoutingContext getRoutingContext() {
		return routingContext;
	}

	JsonObject getBodyAsJson() {
		return new JsonObject(getRoutingContext().getBodyAsString());
	}
}
