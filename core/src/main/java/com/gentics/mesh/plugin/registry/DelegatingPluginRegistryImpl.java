package com.gentics.mesh.plugin.registry;

import static com.gentics.mesh.core.rest.plugin.PluginStatus.FAILED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.INITIALIZED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.AuthServicePluginRegistry;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphql.plugin.GraphQLPluginRegistry;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central plugin registry which delegates to other registry and handles plugin pre-registration.
 */
@Singleton
public class DelegatingPluginRegistryImpl implements DelegatingPluginRegistry {

	private static final Logger log = LoggerFactory.getLogger(DelegatingPluginRegistryImpl.class);

	private final GraphQLPluginRegistry graphqlRegistry;

	private final RestPluginRegistry restRegistry;

	private final AuthServicePluginRegistry authServiceRegistry;

	private final Stack<MeshPlugin> preRegisteredPlugins = new Stack<>();

	private final MeshOptions options;

	private final Lazy<MeshPluginManager> manager;

	private final Lazy<Vertx> vertx;

	private long timerId = -1;

	private final Database db;

	private final PluginDeploymentLock pluginLock;

	@Inject
	public DelegatingPluginRegistryImpl(MeshOptions options, RestPluginRegistry restRegistry, GraphQLPluginRegistry graphqlRegistry,
		AuthServicePluginRegistry authServiceRegistry, Lazy<MeshPluginManager> manager, Lazy<Vertx> vertx, Database db,
		PluginDeploymentLock pluginLock) {
		this.options = options;
		this.restRegistry = restRegistry;
		this.graphqlRegistry = graphqlRegistry;
		this.authServiceRegistry = authServiceRegistry;
		this.manager = manager;
		this.vertx = vertx;
		this.db = db;
		this.pluginLock = pluginLock;
	}

	@Override
	public void start() {
		// Check to avoid bogus restarts
		if (timerId == -1) {
			timerId = vertx.get().setPeriodic(1000, id -> register());
		}
	}

	@Override
	public void stop() {
		if (timerId != -1) {
			vertx.get().cancelTimer(timerId);
			timerId = -1;
		}
		preRegisteredPlugins.clear();
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		String id = plugin.id();
		log.debug("Deregistering plugin {}", id);
		return registries().flatMapCompletable(r -> r.deregister(plugin)).doOnComplete(() -> {
			preRegisteredPlugins.remove(plugin);
		});
	}

	@Override
	public synchronized void checkForConflict(MeshPlugin plugin) {
		for (PluginRegistry registry : Arrays.asList(graphqlRegistry, restRegistry)) {
			registry.checkForConflict(plugin);
		}
	}

	@Override
	public void preRegister(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		manager.get().setStatus(plugin.id(), PluginStatus.PRE_REGISTERED);
		preRegisteredPlugins.add(plugin);
	}

	private Observable<PluginRegistry> registries() {
		return Observable.fromArray(graphqlRegistry, restRegistry, authServiceRegistry);
	}

	/**
	 * Register all currently pre registered plugins.
	 */
	private void register() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking registration of pre-registered plugins");
		}

		long timeout = getPluginTimeout().getSeconds();

		if (preRegisteredPlugins.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No pre-registered plugins found.");
			}
		}
		while (!preRegisteredPlugins.isEmpty()) {
			if (options.getClusterOptions().isEnabled()) {
				if (!db.clusterManager().isWriteQuorumReached()) {
					log.debug("Write quorum not reached. Skipping initialization of pre-registered plugins.");
					return;
				}
			}

			// Use the lock to prevent concurrent inits in clustered setups.
			PluginDeploymentLock lock = pluginLock.lock();
			MeshPlugin plugin = preRegisteredPlugins.pop();
			String id = plugin.id();
			if (log.isDebugEnabled()) {
				log.debug("Invoking initialization of plugin {" + id + "}");
			}
			try {
				plugin.initialize().timeout(timeout, TimeUnit.SECONDS).doOnComplete(() -> {
					manager.get().setStatus(id, INITIALIZED);
				}).andThen(register(plugin).doOnComplete(() -> {
					manager.get().setStatus(id, REGISTERED);
				})).subscribe(() -> {
					lock.close();
					log.info("Completed handling of pre-registered plugin {" + id + "}");
					manager.get().setStatus(id, REGISTERED);
				}, err -> {
					lock.close();
					if (err instanceof TimeoutException) {
						log.error("The registration of plugin {" + id + "} did not complete within {" + timeout
							+ "} seconds. Unloading plugin.");
					} else {
						log.error("Plugin init and register failed for plugin {" + id + "}", err);
					}
					manager.get().setStatus(id, FAILED);
				});
			} catch (Throwable t) {
				log.error("Plugin {" + id + "} failed to initialize since the plugin lock threw an error.", t);
				lock.close();
				manager.get().setStatus(id, FAILED);
			}
		}

	}

	private Duration getPluginTimeout() {
		int timeoutInSeconds = options.getPluginTimeout();
		return Duration.ofSeconds(timeoutInSeconds);
	}

	private Completable register(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		log.debug("Registering plugin {}", plugin.id());
		return registries().flatMapCompletable(r -> r.register(plugin));
	}

}
