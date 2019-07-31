package com.gentics.mesh.plugin.registry;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.router.PluginRouter;
import com.gentics.mesh.router.RouterStorage;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

@Singleton
public class RestPluginRegistry implements PluginRegistry {

	private static final Logger log = LoggerFactory.getLogger(RestPluginRegistry.class);

	/**
	 * Set which is used to detect conflicting deployments.
	 */
	private static Set<String> apiNameSyncSet = Collections.synchronizedSet(new HashSet<>());

	@Inject
	public RestPluginRegistry() {
	}

	@Override
	public Completable register(MeshPlugin plugin) {
		return Completable.create(sub -> {
			if (plugin instanceof RestPlugin) {
				RestPlugin restPlugin = ((RestPlugin) plugin);
				apiNameSyncSet.add(restPlugin.apiName());
				String name = plugin.name();
				String apiName = restPlugin.apiName();
				log.info("Registering rest plugin {" + name + "} with id {" + plugin.id() + "}");
				for (RouterStorage rs : RouterStorage.getInstances()) {
					PluginRouter globalPluginRouter = rs.root().apiRouter().pluginRouter();
					PluginRouter projectPluginRouter = rs.root().apiRouter().projectsRouter().projectRouter().pluginRouter();

					Router globalRouter = globalPluginRouter.getRouter(apiName);
					Router projectRouter = projectPluginRouter.getRouter(apiName);
					log.info("Registering REST API Plugin {" + name + "}");
					restPlugin.registerEndpoints(globalRouter, projectRouter);
				}
			}

			sub.onComplete();
		}).doOnError(error -> {
			if (error instanceof GenericRestException) {
				String key = ((GenericRestException) error).getI18nKey();
				// Don't remove the syncset entry if the plugin has already been deployed.
				if ("admin_plugin_error_plugin_already_deployed".equals(key)) {
					return;
				}
			}
			if (plugin instanceof RestPlugin) {
				apiNameSyncSet.remove(((RestPlugin) plugin).apiName());
			}

		});

	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		return Completable.create(sub -> {
			if (plugin instanceof RestPlugin) {
				RestPlugin restPlugin = (RestPlugin) plugin;
				String name = plugin.name();
				log.info("Deregistering {" + name + "} rest plugin.");
				String apiName = restPlugin.apiName();
				for (RouterStorage rs : RouterStorage.getInstances()) {
					PluginRouter globalPluginRouter = rs.root().apiRouter().pluginRouter();
					PluginRouter projectPluginRouter = rs.root().apiRouter().projectsRouter().projectRouter().pluginRouter();

					// Routers can't be deleted so we need to just clear them of any routes.
					globalPluginRouter.getRouter(apiName).clear();
					projectPluginRouter.getRouter(apiName).clear();
				}
				apiNameSyncSet.remove(apiName);
			}
			sub.onComplete();
		});
	}

	@Override
	public void checkForConflict(MeshPlugin plugin) {
		if (plugin instanceof RestPlugin) {
			RestPlugin restPlugin = (RestPlugin) plugin;
			String apiName = restPlugin.apiName();
			String name = plugin.name();
			if (apiNameSyncSet.contains(apiName)) {
				GenericRestException error = error(BAD_REQUEST, "admin_plugin_error_plugin_already_deployed", name, apiName);
				log.error("The plugin {" + name + "} can't be deployed because another plugin already uses the same apiName {" + apiName + "}",
					error);
				throw error;
			}
		}
	}
}
