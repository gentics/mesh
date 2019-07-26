package com.gentics.mesh.graphql.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.GraphQLPlugin;

@Singleton
public class PluginTypeRegistry {

	private Map<String, GraphQLPlugin> plugins = new HashMap<>();

	@Inject
	public PluginTypeRegistry() {
	}

	public void register(GraphQLPlugin plugin) {
		plugins.put(plugin.id(), plugin);
	}

	public void unregister(GraphQLPlugin plugin) {
		plugins.remove(plugin.id());
	}

	public Set<GraphQLPlugin> getPlugins() {
		return plugins.values().stream().collect(Collectors.toSet());
	}
}
