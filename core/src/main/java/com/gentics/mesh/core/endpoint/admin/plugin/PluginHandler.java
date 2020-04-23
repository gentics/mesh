package com.gentics.mesh.core.endpoint.admin.plugin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Rest API handler for plugin related tasks.
 */
@Singleton
public class PluginHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	private final MeshPluginManager manager;

	private final HandlerUtilities utils;

	private final WriteLock writeLock;

	@Inject
	public PluginHandler(MeshPluginManager manager, HandlerUtilities utils, WriteLock writeLock) {
		this.manager = manager;
		this.utils = utils;
		this.writeLock = writeLock;
	}

	public void handleRead(InternalActionContext ac, String id) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			PluginWrapper pluginWrapper = manager.getPlugin(id);
			if (pluginWrapper == null) {
				throw error(NOT_FOUND, "admin_plugin_error_plugin_not_found", id);
			}
			Plugin plugin = pluginWrapper.getPlugin();
			if (plugin instanceof MeshPlugin) {
				return manager.toResponse((MeshPlugin)plugin);
			}
			throw error(INTERNAL_SERVER_ERROR, "admin_plugin_error_wrong_type");
		}, model -> ac.send(model, CREATED));
	}

	public void handleDeploy(InternalActionContext ac) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				if (!ac.getUser().hasAdminRole()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				PluginDeploymentRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), PluginDeploymentRequest.class);
				String path = requestModel.getPath();
				return manager.deploy(path).map(pluginId -> {
					log.debug("Deployed plugin with deployment name {" + path + "} - Id {" + pluginId + "}");
					PluginWrapper pluginWrapper = manager.getPlugin(pluginId);
					if (pluginWrapper == null) {
						log.error("The plugin was deployed but it could not be found by the manager. It seems that the plugin registration failed.");
						throw error(NOT_FOUND, "admin_plugin_error_plugin_not_found", pluginId);
					}
					Plugin plugin = pluginWrapper.getPlugin();
					if (plugin instanceof MeshPlugin) {
						MeshPlugin meshPlugin = (MeshPlugin)plugin;
						return manager.toResponse(meshPlugin);
					}
					throw error(INTERNAL_SERVER_ERROR, "admin_plugin_error_wrong_type");
				}).blockingGet();
			}, model -> ac.send(model, CREATED));
		}
	}

	public void handleUndeploy(InternalActionContext ac, String uuid) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				if (!ac.getUser().hasAdminRole()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				if (StringUtils.isEmpty(uuid)) {
					throw error(BAD_REQUEST, "admin_plugin_error_uuid_missing");
				}

				return manager
					.undeploy(uuid)
					.toSingleDefault(message(ac, "admin_plugin_undeployed", uuid)).blockingGet();
			}, model -> ac.send(model, OK));
		}
	}

	public void handleReadList(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			List<MeshPlugin> startedPlugins = manager.getStartedMeshPlugins();
			PluginListResponse response = new PluginListResponse();
			Page<PluginResponse> page = new DynamicStreamPageImpl<>(startedPlugins.stream().map(manager::toResponse),
				ac.getPagingParameters());
			page.setPaging(response);
			response.getData().addAll(page.getWrappedList());
			return response;
		}, model -> ac.send(model, OK));
	}

}
