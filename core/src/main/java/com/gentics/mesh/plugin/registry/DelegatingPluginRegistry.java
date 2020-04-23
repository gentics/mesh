package com.gentics.mesh.plugin.registry;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.AuthServicePluginRegistry;
import com.gentics.mesh.etc.config.MeshOptions;
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

	private final AuthServicePluginRegistry authServiceRegistry;

	private final List<MeshPlugin> preRegisteredPlugins = new ArrayList<>();

	private final MeshOptions options;

	@Inject
	public DelegatingPluginRegistry(MeshOptions options, RestPluginRegistry restRegistry, GraphQLPluginRegistry graphqlRegistry,
		AuthServicePluginRegistry authServiceRegistry) {
		this.options = options;
		this.restRegistry = restRegistry;
		this.graphqlRegistry = graphqlRegistry;
		this.authServiceRegistry = authServiceRegistry;
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
		return Observable.fromArray(graphqlRegistry, restRegistry, authServiceRegistry);
	}

	public void preRegister(MeshPlugin plugin) {
		preRegisteredPlugins.add(plugin);
	}

	/**
	 * Register all currently pre registered plugins.
	 */
	public void register() {
		long timeout = getPluginTimeout().getSeconds();
		for (MeshPlugin plugin : preRegisteredPlugins) {
			// TODO check whether quorum is reached
			String id = plugin.id();
			plugin.initialize().andThen(register(plugin)).timeout(timeout, TimeUnit.SECONDS).subscribe(() -> {
				preRegisteredPlugins.remove(plugin);
			}, err -> {
				if (err instanceof TimeoutException) {
					log.error("The registration of plugin {" + id + "} did not complete within {" + timeout
						+ "} seconds. Unloading plugin.");
					throw error(INTERNAL_SERVER_ERROR, "admin_plugin_error_timeout", id);
				}
			});
		}
	}

	public Duration getPluginTimeout() {
		int timeoutInSeconds = options.getPluginTimeout();
		return Duration.ofSeconds(timeoutInSeconds);
	}
}
