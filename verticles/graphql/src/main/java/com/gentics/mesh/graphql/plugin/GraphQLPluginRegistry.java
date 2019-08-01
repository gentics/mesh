package com.gentics.mesh.graphql.plugin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;
import com.gentics.mesh.plugin.registry.PluginRegistry;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class GraphQLPluginRegistry implements PluginRegistry {

	private static final Logger log = LoggerFactory.getLogger(GraphQLPluginRegistry.class);

	/**
	 * Set which is used to detect conflicting deployments.
	 */
	private static Set<String> apiNameSyncSet = Collections.synchronizedSet(new HashSet<>());

	private Map<String, GraphQLPlugin> plugins = new HashMap<>();

	@Inject
	public GraphQLPluginRegistry() {
	}

	@Override
	public Completable register(MeshPlugin plugin) {
		return Completable.fromAction(() -> {
			if (plugin instanceof GraphQLPlugin) {
				GraphQLPlugin graphQLPlugin = (GraphQLPlugin) plugin;
				plugins.put(graphQLPlugin.id(), graphQLPlugin);
			}
		});
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		return Completable.fromAction(() -> {
			if (plugin instanceof GraphQLPlugin) {
				GraphQLPlugin graphQLPlugin = (GraphQLPlugin) plugin;
				plugins.remove(graphQLPlugin.id(), graphQLPlugin);
			}
		});
	}

	public Set<GraphQLPlugin> getPlugins() {
		return plugins.values().stream().collect(Collectors.toSet());
	}

	@Override
	public void checkForConflict(MeshPlugin plugin) {
		if (plugin instanceof GraphQLPlugin) {
			GraphQLPlugin restPlugin = (GraphQLPlugin) plugin;
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
