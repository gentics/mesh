package com.gentics.mesh.plugin.registry;

import java.util.Arrays;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.plugin.GraphQLPluginRegistry;
import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central plugin registry which delegates to other registry.
 */
@Singleton
public class DelegatingPluginRegistry implements PluginRegistry {

	private static final Logger log = LoggerFactory.getLogger(DelegatingPluginRegistry.class);

	private final GraphQLPluginRegistry graphqlRegistry;

	private final RestPluginRegistry restRegistry;

	@Inject
	public DelegatingPluginRegistry(RestPluginRegistry restRegistry, GraphQLPluginRegistry graphqlRegistry) {
		this.restRegistry = restRegistry;
		this.graphqlRegistry = graphqlRegistry;
	}

	@Override
	public Completable register(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		log.debug("Registering plugin {}", plugin.id());
		return registries().flatMapCompletable(r -> r.register(plugin));
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		log.debug("Deregistering plugin {}", plugin.id());
		return registries().flatMapCompletable(r -> r.deregister(plugin));
	}

	/**
	 * Check whether any other plugin already occupies the api name of the given plugin.
	 * 
	 * @param plugin
	 */
	@Override
	public synchronized void checkForConflict(MeshPlugin plugin) {
		for (PluginRegistry registry : Arrays.asList(graphqlRegistry, restRegistry)) {
			registry.checkForConflict(plugin);
		}
	}

	private Observable<PluginRegistry> registries() {
		return Observable.fromArray(graphqlRegistry, restRegistry);
	}
}
