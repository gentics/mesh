package com.gentics.mesh.plugin.manager;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.error.Errors.rxError;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.LOADED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.STARTED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.VALIDATED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.map.HashedMap;
import org.pf4j.AbstractPluginManager;
import org.pf4j.CompoundPluginLoader;
import org.pf4j.CompoundPluginRepository;
import org.pf4j.DefaultExtensionFactory;
import org.pf4j.DefaultExtensionFinder;
import org.pf4j.DefaultPluginStatusProvider;
import org.pf4j.DefaultVersionManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.ExtensionFinder;
import org.pf4j.JarPluginLoader;
import org.pf4j.JarPluginRepository;
import org.pf4j.Plugin;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginFactory;
import org.pf4j.PluginLoader;
import org.pf4j.PluginRepository;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStatusProvider;
import org.pf4j.PluginWrapper;
import org.pf4j.VersionManager;
import org.pf4j.util.FileUtils;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.impl.MeshPluginDescriptorFinderImpl;
import com.gentics.mesh.plugin.impl.MeshPluginDescriptorImpl;
import com.gentics.mesh.plugin.pf4j.MeshPluginFactory;
import com.gentics.mesh.plugin.registry.DelegatingPluginRegistry;
import com.gentics.mesh.plugin.util.PluginUtils;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The implementation of an {@link MeshPluginManager}.
 */
@Singleton
public class MeshPluginManagerImpl extends AbstractPluginManager implements MeshPluginManager {

	private static final Logger log = LoggerFactory.getLogger(MeshPluginManagerImpl.class);

	public static final String PLUGINS_DIR_CONFIG_PROPERTY_NAME = "pf4j.pluginsConfigDir";

	private final Lazy<Vertx> vertx;

	private final PluginFactory pluginFactory;

	private final MeshOptions options;

	private final List<PluginWrapper> restartingPlugins = Collections.synchronizedList(new ArrayList<>(0));

	private final List<PluginWrapper> startingPlugins = Collections.synchronizedList(new ArrayList<>(1));

	// We track our own plugin status since the PF4J state is not extendible.
	private final Map<String, PluginStatus> pluginStatusMap = new HashedMap<>();

	private final DelegatingPluginRegistry pluginRegistry;

	@Inject
	public MeshPluginManagerImpl(MeshOptions options, MeshPluginFactory pluginFactory,
			DelegatingPluginRegistry pluginRegistry, Database database, Lazy<Vertx> vertx) {
		this.pluginFactory = pluginFactory;
		this.options = options;
		this.pluginRegistry = pluginRegistry;
		this.vertx = vertx;
		delayedInitialize();
	}

	protected void delayedInitialize() {
		super.initialize();
	}

	protected void registerEventListeners() {
		if (options.getClusterOptions().isEnabled()) {
			EventBus eb = vertx.get().eventBus();

			eb.consumer(CLUSTER_DATABASE_CHANGE_STATUS.address, (Message<JsonObject> handler) -> {
				JsonObject info = handler.body();
				Boolean isMe = info.getBoolean("isMe");

				if (isMe != null && isMe) {
					DB_STATUS status = DB_STATUS.valueOf(info.getString("status"));
					log.info("Received local database status update - " + status.name());

					switch (status) {
					case NOT_AVAILABLE:
					case OFFLINE:
					case SYNCHRONIZING:
					case BACKUP:
						handleDatabaseAvailability(false);
						break;
					case ONLINE:
						handleDatabaseAvailability(true);
						break;
					}
				}
			});
			log.info("Distributed DB status event listener registered.");
		}
	}

	protected synchronized void handleDatabaseAvailability(boolean available) {
		if (available) {
			startPluginsFrom(restartingPlugins);
		} else {
			synchronized (startingPlugins) {
				for (PluginWrapper wrapper : startingPlugins) {
					log.info("Restarting plugin {" + wrapper.getPluginId() + "} because of an unavailable database.");

					if (wrapper.getPlugin() instanceof MeshPlugin) {
						MeshPlugin meshPlugin = (MeshPlugin) wrapper.getPlugin();
						meshPlugin.shutdown();
					}
					restartingPlugins.add(wrapper);
				}
				startingPlugins.clear();
			}
		}
	}
	
	@Override
	public void init() {
		registerEventListeners();
	}

	@Override
	protected void initialize() {
		// Don't invoke super init here since we need to do this after dagger has
		// injected the dependencies.
		log.info("PF4J version {} in '{}' mode", getVersion(), getRuntimeMode());
	}

	@Override
	public void start() {
		String base = options.getPluginDirectory();
		if (base == null) {
			base = "plugins";
		}
		log.trace("Using base dir {" + base + "}");
		pluginsRoot = Paths.get(base);
		// Re-create the plugin repo since the root dir might have changed
		pluginRepository = createPluginRepository();
		pluginRegistry.start();
	}

	@Override
	public Completable deployExistingPluginFiles() {
		return Completable.defer(() -> {
			try {
				loadPlugins();
			} catch (Throwable e) {
				log.error("Error while loading plugin.", e);
				return Completable.error(new RuntimeException("Error while loading plugins.", e));
			}
			try {
				startPlugins();
			} catch (Throwable e) {
				log.error("Error while starting plugin.", e);
				return Completable.error(new RuntimeException("Error while starting plugins.", e));
			}
			return Completable.complete();
		});
	}

	@Override
	public void startPlugins() {
		startPluginsFrom(resolvedPlugins);
	}

	private void startPluginsFrom(List<PluginWrapper> pluginStorage) {
		for (PluginWrapper pluginWrapper : pluginStorage) {
			PluginState pluginState = pluginWrapper.getPluginState();
			if ((PluginState.DISABLED != pluginState) && (PluginState.STARTED != pluginState)) {
				try {
					log.info("Start plugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
					Plugin plugin = pluginWrapper.getPlugin();
					plugin.start();
					// Set state for PF4J
					pluginWrapper.setPluginState(PluginState.STARTED);

					registerMeshPlugin(pluginWrapper.getPluginId());
				} catch (Throwable e) {
					log.error("Error while starting plugins " + e.getMessage(), e);
				}
			}
		}
		if (pluginStorage == restartingPlugins) {
			pluginStorage.clear();
		}
	}

	/**
	 * Start the specified plugin and its dependencies.
	 */
	@Override
	public PluginState startPlugin(String pluginId) {
		if (!plugins.containsKey(pluginId)) {
			throw new IllegalArgumentException(String.format("Unknown pluginId %s", pluginId));
		}

		PluginWrapper pluginWrapper = getPlugin(pluginId);
		PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
		PluginState pluginState = pluginWrapper.getPluginState();
		if (PluginState.STARTED == pluginState) {
			log.debug("Already started plugin '{}'", getPluginLabel(pluginDescriptor));
			return PluginState.STARTED;
		}

		if (!resolvedPlugins.contains(pluginWrapper)) {
			log.warn("Cannot start an unresolved plugin '{}'", getPluginLabel(pluginDescriptor));
			return pluginState;
		}

		// TODO Are we allowed to enable plugins by ourselves?
		// Enabling non-Mesh plugins left for backwards compatibility.
		if (PluginState.DISABLED == pluginState) {
			if (pluginWrapper.getPlugin() instanceof MeshPlugin) {
				log.warn("Cannot start a disabled Mesh plugin '{}'", getPluginLabel(pluginDescriptor));
				return pluginState;
			} else {
				// automatically enable plugin on manual plugin start
				if (!enablePlugin(pluginId)) {
					return pluginState;
				}
			}
		}

		for (PluginDependency dependency : pluginDescriptor.getDependencies()) {
			startPlugin(dependency.getPluginId());
		}

		log.info("Start plugin '{}'", getPluginLabel(pluginDescriptor));
		pluginWrapper.getPlugin().start();
		pluginWrapper.setPluginState(PluginState.STARTED);

		// MeshPlugins have to be registered before setting as Started
		if (!(pluginWrapper.getPlugin() instanceof MeshPlugin)) {
			startedPlugins.add(pluginWrapper);
			firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
		}

		return pluginWrapper.getPluginState();
	}

	@Override
	public Single<String> deploy(Path path) {
		Objects.requireNonNull(path, "The path must not be null");
		log.debug("Deploying file {" + path + "}");

		// 1. Initial checks
		String name = path.getFileName().toString();
		if (Files.notExists(path)) {
			return rxError(BAD_REQUEST, "admin_plugin_error_plugin_deployment_failed", name);
		}

		// 2. Load plugin into p4fj
		String id;
		try {
			id = loadPlugin(path);
		} catch (PluginRuntimeException e) {
			if (e.getMessage().startsWith("There is an already loaded plugin")) {
				log.error("Plugin deployment of {" + name + "} failed.", e);
				return rxError(BAD_REQUEST, "admin_plugin_error_plugin_with_id_already_deployed", name);
			} else {
				log.error("Plugin deployment of {" + name + "} failed.", e);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
			}
		} catch (Throwable e) {
			log.error("Plugin deployment of {" + name + "} failed.", e);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
		}
		if (id == null) {
			log.warn(
					"The plugin was not registered after deployment. Maybe the initialisation failed. Going to unload the plugin.");
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_did_not_register", name);
		}
		setStatus(id, LOADED);

		// 3. Invoke the validation of the plugin class
		try {
			PluginWrapper plugin = getPlugin(id);
			if (plugin == null || plugin.getPlugin() == null) {
				log.error("The plugin {" + path + "/" + id + "} could not be loaded.");
				plugins.remove(id);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
			}
			validate(plugin.getPlugin());
			setStatus(id, VALIDATED);
		} catch (GenericRestException e) {
			log.error("Post start validation of plugin {" + path + "/" + id + "} failed.", e);
			removePlugin(id);
			throw e;
		} catch (Throwable e) {
			log.error("Error while loading plugin class", e);
			removePlugin(id);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
		}

		// 4. Start the plugin
		try {
			startPlugin(id);
			setStatus(id, STARTED);
		} catch (GenericRestException e) {
			log.error("Starting of plugin {" + path + "/" + id + "} failed.", e);
			rollback(id);
			throw e;
		} catch (Throwable e) {
			log.error("Starting of plugin {" + path + "/" + id + "} failed.", e);
			rollback(id);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_starting_failed", name);
		}

		// 5. Pre-Register the plugin
		try {
			registerMeshPlugin(id);
		} catch (Throwable e) {
			log.error("Plugin registration failed with error", e);
			rollback(id);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_did_not_register");
		}

		return Single.just(id);
	}

	private void registerMeshPlugin(String id) {
		PluginWrapper wrapper = getPlugin(id);
		Plugin plugin = wrapper.getPlugin();

		if (plugin instanceof MeshPlugin) {
			startingPlugins.add(wrapper);

			PluginState oldState = wrapper.getPluginState();

			MeshPlugin meshPlugin = (MeshPlugin) plugin;
			pluginRegistry.preRegister(meshPlugin);

			startingPlugins.remove(wrapper);
			startedPlugins.add(wrapper);
			firePluginStateEvent(new PluginStateEvent(this, wrapper, oldState));
		}
	}

	private void removePlugin(String id) {
		plugins.remove(id);
		pluginStatusMap.remove(id);
	}

	/**
	 * Try to unload the plugin with the id.
	 * 
	 * @param id
	 * @param true if the plugin was unloaded - Otherwise false
	 */
	private boolean rollback(String id) {
		try {
			unloadPlugin(id);
			removePlugin(id);
			return true;
		} catch (Throwable e2) {
			log.error("Error while unloading plugin {" + id + "}", e2);
			return false;
		}
	}

	@Override
	public Completable undeploy(String id) {
		return Completable.fromRunnable(() -> {
			resolvePlugins();
			PluginWrapper wrapper = getPlugin(id);
			if (wrapper == null) {
				log.debug("Plugin with id {} could not be found", id);
				return;
			}
			Plugin plugin = wrapper.getPlugin();
			if (plugin instanceof MeshPlugin) {
				try {
					MeshPlugin meshPlugin = (MeshPlugin) plugin;
					withTimeout(id, "shudown", pluginRegistry.deregister(meshPlugin).andThen(meshPlugin.shutdown()));
				} catch (Throwable t) {
					log.error("Error while calling shutdown of plugin. Trying to unload anyway.", t);
				}
			}
			unloadPlugin(id);
		});
	}

	@Override
	public void validate(Plugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		if (!MeshPlugin.class.isAssignableFrom(plugin.getClass())) {
			throw error(BAD_REQUEST, "admin_plugin_error_wrong_type");
		} else {
			MeshPlugin meshPlugin = (MeshPlugin) plugin;
			pluginRegistry.checkForConflict(meshPlugin);
			PluginUtils.validate(meshPlugin.getManifest());
			PluginUtils.validate(meshPlugin);
		}
	}

	@Override
	public SortedMap<String, MeshPlugin> getPluginsMap() {
		SortedMap<String, MeshPlugin> sortedMap = new TreeMap<>();
		getStartedPlugins().forEach(pw -> {
			sortedMap.put(pw.getPluginId(), (MeshPlugin) pw.getPlugin());
		});
		return sortedMap;
	}

	@Override
	public Completable stop() {
		return Completable.fromRunnable(() -> {
			pluginRegistry.stop();
			stopPlugins();
			unloadPlugins();
		});
	}

	@Override
	public void unloadPlugins() {
		for (PluginWrapper wrapper : getPlugins()) {
			Plugin plugin = wrapper.getPlugin();
			if (plugin instanceof MeshPlugin) {
				MeshPlugin meshPlugin = (MeshPlugin) plugin;
				try {
					withTimeout(meshPlugin.id(), "shtdown",
							pluginRegistry.deregister((MeshPlugin) plugin).andThen(meshPlugin.shutdown()));
				} catch (Exception e) {
					log.error("Shutdown call of plugin {" + meshPlugin.id() + "} failed. Unloading anyway.", e);
				}
			}
			undeploy(wrapper.getPluginId());
			unloadPlugin(wrapper.getPluginId());
		}
	}

	@Override
	protected PluginDescriptorFinder createPluginDescriptorFinder() {
		return new MeshPluginDescriptorFinderImpl();
	}

	private PluginWrapper loadPlugin(Class<?> clazz, String id) {
		MeshPluginDescriptor pluginDescriptor = new MeshPluginDescriptorImpl(clazz, id);
		log.debug("Found descriptor {}", pluginDescriptor);
		String pluginClassName = clazz.getName();
		log.debug("Class '{}' for plugin", pluginClassName);

		PluginClassLoader pluginClassLoader = new PluginClassLoader(this, pluginDescriptor,
				getClass().getClassLoader());

		// create the plugin wrapper
		log.debug("Creating wrapper for plugin '{}'", pluginClassName);
		PluginWrapper pluginWrapper = new PluginWrapper(this, pluginDescriptor, null, pluginClassLoader);
		pluginWrapper.setPluginFactory(getPluginFactory());

		// test for disabled plugin
		if (isPluginDisabled(pluginDescriptor.getPluginId())) {
			log.info("Plugin '{}' is disabled", pluginClassName);
			pluginWrapper.setPluginState(PluginState.DISABLED);
		}

		// validate the plugin
		if (!isPluginValid(pluginWrapper)) {
			log.warn("Plugin '{}' is invalid and it will be disabled", pluginClassName);
			pluginWrapper.setPluginState(PluginState.DISABLED);
		}

		log.debug("Created wrapper '{}' for plugin '{}'", pluginWrapper, pluginClassName);

		// add plugin to the list with plugins
		plugins.put(id, pluginWrapper);

		getUnresolvedPlugins().add(pluginWrapper);

		// add plugin class loader to the list with class loaders
		getPluginClassLoaders().put(id, pluginClassLoader);

		resolvePlugins();

		return pluginWrapper;
	}

	@Override
	public Completable deploy(Class<?> clazz, String id) {
		Objects.requireNonNull(id, "A plugin must have a unique id (e.g. hello-world)");
		if (!isMeshPlugin(clazz)) {
			return rxError(BAD_REQUEST, "admin_plugin_error_wrong_type").ignoreElement();
		}
		String name = clazz.getSimpleName();
		return Completable.defer(() -> {
			log.debug("Deploying plugin class {" + name + "}");

			// 1. Load plugin
			try {
				PluginDescriptor desc = loadPlugin(clazz, id).getDescriptor();
				if (!(desc instanceof MeshPluginDescriptor)) {
					return rxError(INTERNAL_SERVER_ERROR, "plugin_desc_wrong").ignoreElement();
				}
				setStatus(id, LOADED);
			} catch (Throwable e) {
				log.error("Error while deploying plugin via class", e);
				return Completable.error(new RuntimeException("Error while deploying plugin {" + clazz + "}", e));
			}

			// 2. Validate plugin
			try {
				PluginWrapper wrapper = getPlugin(id);
				if (wrapper == null || wrapper.getPlugin() == null) {
					log.error("The plugin {" + name + "/" + id + "} could not be loaded.");
					removePlugin(id);
					return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name)
							.ignoreElement();
				}
				validate(wrapper.getPlugin());
				setStatus(id, VALIDATED);
			} catch (GenericRestException e) {
				log.error("Post start validation of plugin {" + name + "/" + id + "} failed.", e);
				removePlugin(id);
				return Completable.error(e);
			} catch (Throwable e) {
				log.error("Error while loading plugin class", e);
				removePlugin(id);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name).ignoreElement();
			}

			// 3. Start plugin
			try {
				startPlugin(id);
			} catch (Throwable e) {
				log.error("Error while starting plugin", e);
				rollback(id);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_starting_failed", name)
						.ignoreElement();
			}

			// 4. Register plugin
			try {
				registerMeshPlugin(id);
			} catch (Throwable e) {
				log.error("Plugin registration failed with error", e);
				try {
					stopPlugin(id);
				} catch (Exception e2) {
					log.error("Error while stopping failed plugin. Directly unloading it.", e2);
				}
				rollback(id);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_did_not_register").ignoreElement();
			}

			return Completable.complete();
		});

	}

	@Override
	public void setStatus(String id, PluginStatus status) {
		pluginStatusMap.put(id, status);
	}

	@Override
	public PluginStatus getStatus(String id) {
		return pluginStatusMap.get(id);
	}

	private void withTimeout(String id, String operationName, Completable op) {
		long timeout = getPluginTimeout().getSeconds();
		try {
			op.timeout(timeout, TimeUnit.SECONDS).blockingAwait();
		} catch (Throwable t) {
			if (t instanceof TimeoutException) {
				log.error("The registration of plugin {" + id + "} did not complete within {" + timeout
						+ "} seconds. Unloading plugin.");
				throw error(INTERNAL_SERVER_ERROR, "admin_plugin_error_timeout", id);
			}
			throw t;
		}
	}

	private boolean isMeshPlugin(Class<?> clazz) {
		return MeshPlugin.class.isAssignableFrom(clazz);
	}

	@Override
	public Path getPluginsRoot() {
		return Paths.get(options.getPluginDirectory());
	}

	@Override
	public Set<String> getPluginIds() {
		return super.getStartedPlugins().stream().map(pw -> pw.getPluginId()).collect(Collectors.toSet());
	}

	@Override
	public List<MeshPlugin> getStartedMeshPlugins() {
		return getStartedPlugins().stream().map(w -> (MeshPlugin) w.getPlugin()).collect(Collectors.toList());
	}

	@Override
	public Duration getPluginTimeout() {
		int timeoutInSeconds = options.getPluginTimeout();
		return Duration.ofSeconds(timeoutInSeconds);
	}

	/**
	 * Load a plugin from disk. If the path is a zip file, first unpack.
	 *
	 * @param pluginPath plugin location on disk
	 * @return PluginWrapper for the loaded plugin or null if not loaded
	 * @throws PluginRuntimeException if problems during load
	 */
	@Override
	protected PluginWrapper loadPluginFromPath(Path pluginPath) {
		// First unzip any ZIP files
		try {
			pluginPath = FileUtils.expandIfZip(pluginPath);
		} catch (Exception e) {
			log.warn("Failed to unzip " + pluginPath, e);
			return null;
		}

		return super.loadPluginFromPath(pluginPath);
	}

	@Override
	protected PluginFactory createPluginFactory() {
		return pluginFactory;
	}

	@Override
	public PluginFactory getPluginFactory() {
		return pluginFactory;
	}

	@Override
	protected VersionManager createVersionManager() {
		return new DefaultVersionManager();
	}

	@Override
	protected ExtensionFactory createExtensionFactory() {
		return new DefaultExtensionFactory();
	}

	@Override
	protected ExtensionFinder createExtensionFinder() {
		DefaultExtensionFinder extensionFinder = new DefaultExtensionFinder(this);
		addPluginStateListener(extensionFinder);
		return extensionFinder;
	}

	@Override
	protected PluginStatusProvider createPluginStatusProvider() {
		String configDir = System.getProperty(PLUGINS_DIR_CONFIG_PROPERTY_NAME);
		Path configPath = configDir != null ? Paths.get(configDir) : getPluginsRoot();

		return new DefaultPluginStatusProvider(configPath);
	}

	@Override
	protected PluginRepository createPluginRepository() {
		return new CompoundPluginRepository().add(new JarPluginRepository(getPluginsRoot()), this::isNotDevelopment);
	}

	@Override
	protected PluginLoader createPluginLoader() {
		return new CompoundPluginLoader().add(new JarPluginLoader(this), this::isNotDevelopment);
	}

	@Override
	public PluginResponse toResponse(MeshPlugin plugin) {
		PluginResponse response = plugin.toResponse();
		PluginStatus status = getStatus(plugin.id());
		response.setStatus(status);
		return response;
	}

}
