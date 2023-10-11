package com.gentics.mesh.plugin.registry;

import static com.gentics.mesh.core.rest.plugin.PluginStatus.INITIALIZED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.AuthServicePluginRegistry;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.plugin.PluginEventModel;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.plugin.GraphQLPluginRegistry;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

/**
 * Central plugin registry which delegates to other registry and handles plugin pre-registration.
 */
@Singleton
public class DelegatingPluginRegistryImpl implements DelegatingPluginRegistry {

	public static final String GLOBAL_PLUGIN_LOCK_KEY = "MESH_PLUGIN_REGISTRATION_LOCK";

	private static final Logger log = LoggerFactory.getLogger(DelegatingPluginRegistryImpl.class);

	private final GraphQLPluginRegistry graphqlRegistry;

	private final RestPluginRegistry restRegistry;

	private final AuthServicePluginRegistry authServiceRegistry;

	private final MeshOptions options;

	private final Lazy<MeshPluginManager> manager;

	private final Lazy<Vertx> rxVertx;

	private final Database db;

	private MessageConsumer<Object> clusterConsumer;
	private MessageConsumer<Object> preRegisterConsumer;

	@Inject
	public DelegatingPluginRegistryImpl(MeshOptions options, RestPluginRegistry restRegistry, GraphQLPluginRegistry graphqlRegistry,
		AuthServicePluginRegistry authServiceRegistry, Lazy<MeshPluginManager> manager, Lazy<Vertx> rxVertx, Database db) {
		this.options = options;
		this.restRegistry = restRegistry;
		this.graphqlRegistry = graphqlRegistry;
		this.authServiceRegistry = authServiceRegistry;
		this.manager = manager;
		this.rxVertx = rxVertx;
		this.db = db;
	}

	@Override
	public void stop() {
		if (clusterConsumer != null) {
			clusterConsumer.unregister();
		}
		if (preRegisterConsumer != null) {
			preRegisterConsumer.unregister();
		}
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		String id = plugin.id();
		log.debug("Deregistering plugin {}", id);
		return registries().flatMapCompletable(r -> r.deregister(plugin));
	}

	@Override
	public synchronized void checkForConflict(MeshPlugin plugin) {
		for (PluginRegistry registry : Arrays.asList(graphqlRegistry, restRegistry)) {
			registry.checkForConflict(plugin);
		}
	}

	@Override
	public void preRegister(MeshPlugin plugin) {
		EventBus eb = rxVertx.get().eventBus();
		Objects.requireNonNull(plugin, "The plugin must not be null");
		manager.get().setStatus(plugin.id(), PluginStatus.PRE_REGISTERED);
		JsonObject payload = toEventPayload(plugin);
		eb.publish(MeshEvent.PLUGIN_PRE_REGISTERED.getAddress(), payload);
		initAndRegister(plugin);
	}

	private Observable<PluginRegistry> registries() {
		return Observable.fromArray(graphqlRegistry, restRegistry, authServiceRegistry);
	}

	/**
	 * Register all currently pre registered plugins.
	 */
	private void initAndRegister(MeshPlugin plugin) {
		EventBus eb = rxVertx.get().eventBus();
		if (log.isDebugEnabled()) {
			log.debug("Invoking registration of pre-registered plugins");
		}
		long timeout = getPluginTimeout().getSeconds();

		String id = plugin.id();
		JsonObject payload = toEventPayload(plugin);
		optionalDatabaseReadyCheck().andThen(optionalLock(registerAndInitializePlugin(plugin), id)).subscribe(() -> {
			log.info("Completed handling of pre-registered plugin {" + id + "}");
			manager.get().setStatus(id, REGISTERED);

			eb.publish(MeshEvent.PLUGIN_REGISTERED.getAddress(), payload);
			eb.publish(MeshEvent.PLUGIN_DEPLOYED.getAddress(), payload);
		}, err -> {
			if (err instanceof TimeoutException) {
				log.error("The registration of plugin {" + id + "} did not complete within {" + timeout
					+ "} seconds. Unloading plugin.");
			} else {
				log.error("Plugin init and register failed for plugin {" + id + "}", err);
			}
			manager.get().setPluginFailed(id);
			eb.publish(MeshEvent.PLUGIN_DEPLOY_FAILED.getAddress(), payload);
		});

	}

	private Completable registerAndInitializePlugin(MeshPlugin plugin) {
		long timeout = getPluginTimeout().getSeconds();
		String id = plugin.id();
		return plugin.initialize().timeout(timeout, TimeUnit.SECONDS).doOnSubscribe(ignore -> {
			log.trace("Start initialization of plugin {}", id);
		}).doOnComplete(() -> {
			manager.get().setStatus(id, INITIALIZED);
			log.debug("Plugin initialization of plugin {} completed", id);
		}).andThen(register(plugin).doOnComplete(() -> {
			log.debug("Plugin registration of plugin {} completed", id);
			manager.get().setStatus(id, REGISTERED);
		}));
	}

	/**
	 * Use the lock in clustered mode to prevent concurrent inits
	 * 
	 * @param lockedAction
	 * @param id
	 * @return
	 */
	private Completable optionalLock(Completable lockedAction, String id) {
		int timeout = options.getPluginTimeout();
		if (options.getClusterOptions().isEnabled()) {
			return rxVertx.get().sharedData().rxGetLockWithTimeout(GLOBAL_PLUGIN_LOCK_KEY, timeout * 4 * 1000).toMaybe()
				.flatMapCompletable(lock -> {
					log.debug("Acquired lock for registration of plugin {}", id);
					return lockedAction
						.doFinally(() -> {
							log.debug("Releasing lock for plugin {}", id);
							lock.release();
						});
				});
		} else {
			return lockedAction;
		}
	}

	private Completable optionalDatabaseReadyCheck() {
		if (options.getClusterOptions().isEnabled()) {
			return db.clusterManager().waitUntilDistributedDatabaseReady();
		} else {
			return Completable.complete();
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

	private JsonObject toEventPayload(MeshPlugin plugin) {
		PluginEventModel model = new PluginEventModel();
		model.setId(plugin.id());
		model.setOrigin(options.getNodeName());
		return new JsonObject(model.toJson(true));
	}

}
