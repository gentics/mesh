package com.gentics.mesh.plugin.manager;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.error.Errors.rxError;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.pf4j.DefaultPluginManager;
import org.pf4j.Plugin;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginFactory;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.impl.MeshPluginDescriptorFinderImpl;
import com.gentics.mesh.plugin.impl.MeshPluginDescriptorImpl;
import com.gentics.mesh.plugin.pf4j.MeshPluginFactory;
import com.gentics.mesh.plugin.util.PluginUtils;
import com.gentics.mesh.router.PluginRouter;
import com.gentics.mesh.router.RouterStorage;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

@Singleton
public class MeshPluginManagerImpl extends DefaultPluginManager implements MeshPluginManager {

	private static final Logger log = LoggerFactory.getLogger(MeshPluginManagerImpl.class);

	private static Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());

	private final PluginFactory pluginFactory;

	private MeshOptions options;

	@Inject
	public MeshPluginManagerImpl(MeshOptions options, MeshPluginFactory pluginFactory) {
		this.pluginFactory = pluginFactory;
		this.options = options;
		delayedInitialize();
	}

	protected void delayedInitialize() {
		super.initialize();

		addPluginStateListener(event -> {
			if (event.getPluginState().equals(PluginState.STARTED)) {
				Plugin plugin = event.getPlugin().getPlugin();
				if (plugin instanceof RestPlugin) {
					registerPlugin((MeshPlugin) plugin, false).blockingAwait(15, TimeUnit.SECONDS);
				}
			}
			if (event.getPluginState().equals(PluginState.STOPPED)) {
				Plugin plugin = event.getPlugin().getPlugin();
				if (plugin instanceof RestPlugin) {
					deregisterPlugin((MeshPlugin) plugin).blockingAwait(15, TimeUnit.SECONDS);
				}
			}
		});

	}

	@Override
	protected void initialize() {
		// Don't invoke init here since we need to do this after dagger has injected the dependencies.
	}

	private Completable registerPlugin(MeshPlugin plugin, boolean strict) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		return plugin.initialize()
			.andThen(Completable.create(sub -> {
				if (plugin instanceof RestPlugin) {
					RestPlugin restPlugin = ((RestPlugin) plugin);
					syncSet.add(restPlugin.apiName());
					String name = plugin.getName();
					String apiName = restPlugin.apiName();
					log.info("Registering plugin {" + name + "} with id {" + plugin.id() + "}");
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
			})).doOnError(error -> {
				if (error instanceof GenericRestException) {
					String key = ((GenericRestException) error).getI18nKey();
					// Don't remove the syncset entry if the plugin has already been deployed.
					if ("admin_plugin_error_plugin_already_deployed".equals(key)) {
						return;
					}
				}
				if (plugin instanceof RestPlugin) {
					syncSet.remove(((RestPlugin) plugin).apiName());
				}
			});
	}

	private Completable deregisterPlugin(MeshPlugin plugin) {
		return Completable.create(sub -> {
			String name = plugin.getName();
			log.info("Deregistering {" + name + "} plugin.");

			String apiName = plugin.getManifest().getApiName();
			for (RouterStorage rs : RouterStorage.getInstances()) {
				PluginRouter globalPluginRouter = rs.root().apiRouter().pluginRouter();
				PluginRouter projectPluginRouter = rs.root().apiRouter().projectsRouter().projectRouter().pluginRouter();

				// Routers can't be deleted so we need to just clear them of any routes.
				globalPluginRouter.getRouter(apiName).clear();
				projectPluginRouter.getRouter(apiName).clear();
			}
			syncSet.remove(apiName);
			sub.onComplete();
		}).andThen(plugin.prepareStop());
	}

	@Override
	public void init() {
		String base = options.getPluginDirectory();
		if (base == null) {
			base = "plugins";
		}
		log.trace("Using base dir {" + base + "}");
		pluginsRoot = Paths.get(base);
		// Re-create the plugin repo since the root dir might have changed
		pluginRepository = createPluginRepository();
	}

	@Override
	public Completable deployExistingPluginFiles() {
		return Completable.defer(() -> {
			try {
				loadPlugins();
			} catch (Throwable e) {
				return Completable.error(new RuntimeException("Error while loading plugins.", e));
			}
			try {
				startPlugins();
			} catch (Throwable e) {
				return Completable.error(new RuntimeException("Error while starting plugins.", e));
			}
			return Completable.complete();
		});
	}

	@Override
	public Single<String> deploy(Path path) {
		Objects.requireNonNull(path, "The path must not be null");
		log.debug("Deploying file {" + path + "}");

		// Initial checks
		String name = path.getFileName().toString();
		if (Files.notExists(path)) {
			return rxError(BAD_REQUEST, "admin_plugin_error_plugin_deployment_failed", name);
		}

		// Load plugin into p4fj
		String uuid;
		try {
			uuid = loadPlugin(path);
		} catch (Throwable e) {
			log.error("Plugin deployment of {" + name + "} failed.", e);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
		}
		if (uuid == null) {
			log.warn("The plugin was not registered after deployment. Maybe the initialisation failed. Going to unload the plugin.");
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_did_not_register", name);
		}

		// Invoke the loading of the plugin class
		try {
			PluginWrapper plugin = getPlugin(uuid);
			if (plugin == null || plugin.getPlugin() == null) {
				log.error("The plugin {" + path + "/" + uuid + "} could not be loaded.");
				plugins.remove(uuid);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
			}
			validate(plugin.getPlugin(), true);
		} catch (GenericRestException e) {
			log.error("Post start validation of plugin {" + path + "/" + uuid + "} failed.", e);
			plugins.remove(uuid);
			throw e;
		} catch (Throwable e) {
			log.error("Error while loading plugin class", e);
			plugins.remove(uuid);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
		}

		// Start the plugin
		try {
			startPlugin(uuid);
		} catch (GenericRestException e) {
			log.error("Starting of plugin {" + path + "/" + uuid + "} failed.", e);
			tryUnloadPlugin(uuid);
			throw e;
		} catch (Throwable e) {
			log.error("Starting of plugin {" + path + "/" + uuid + "} failed.", e);
			tryUnloadPlugin(uuid);
			return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_starting_failed", name);
		}
		return Single.just(uuid);
	}

	/**
	 * Try to unload the plugin with the id.
	 * 
	 * @param uuid
	 * @param true
	 *            if the plugin was unloaded - Otherwise false
	 */
	private boolean tryUnloadPlugin(String uuid) {
		try {
			unloadPlugin(uuid);
			return true;
		} catch (Throwable e2) {
			log.error("Error while unloading plugin {" + uuid + "}", e2);
			return false;
		}
	}

	@Override
	public Completable undeploy(String uuid) {
		return Completable.fromRunnable(() -> {
			resolvePlugins();
			unloadPlugin(uuid);
		});
	}

	@Override
	public void validate(Plugin plugin, boolean strict) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		if (!MeshPlugin.class.isAssignableFrom(plugin.getClass())) {
			throw error(BAD_REQUEST, "admin_plugin_error_wrong_type");
		} else {
			MeshPlugin meshPlugin = (MeshPlugin) plugin;
			checkForConflict(meshPlugin);
			PluginUtils.validate(meshPlugin.getManifest(), strict);
			PluginUtils.validate(meshPlugin);
		}
	}

	/**
	 * Check whether any other plugin already occupies the api name of the given plugin.
	 * 
	 * @param plugin
	 */
	private synchronized void checkForConflict(MeshPlugin plugin) {
		String apiName = plugin.getManifest().getApiName();
		String name = plugin.getName();
		if (syncSet.contains(apiName)) {
			GenericRestException error = error(BAD_REQUEST, "admin_plugin_error_plugin_already_deployed", name, apiName);
			log.error("The plugin {" + name + "} can't be deployed because another plugin already uses the same apiName {" + apiName + "}", error);
			throw error;
		}
	}

	@Override
	public SortedMap<String, MeshPlugin> getPluginsMap() {
		SortedMap<String, MeshPlugin> sortedMap = new TreeMap<>();
		getPlugins().forEach(pw -> {
			System.out.println(pw.getPluginId() + " " + ((MeshPlugin)pw.getPlugin()).id());
			sortedMap.put(pw.getPluginId(), (MeshPlugin) pw.getPlugin());
		});
		return sortedMap;
	}

	@Override
	public Completable stop() {
		return Completable.fromRunnable(() -> {
			stopPlugins();
			unloadPlugins();
		});
	}

	@Override
	public void unloadPlugins() {
		for (PluginWrapper plugin : getPlugins()) {
			undeploy(plugin.getPluginId());
			unloadPlugin(plugin.getPluginId());
		}
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
	protected PluginDescriptorFinder createPluginDescriptorFinder() {
		return new MeshPluginDescriptorFinderImpl();
	}

	private PluginWrapper loadPlugin(Class<?> clazz, String id) {
		MeshPluginDescriptor pluginDescriptor = new MeshPluginDescriptorImpl(clazz, id);
		String pluginUuid = pluginDescriptor.getUuid();
		log.debug("Found descriptor {}", pluginDescriptor);
		String pluginClassName = clazz.getName();
		log.debug("Class '{}' for plugin", pluginClassName);

		// create the plugin wrapper
		log.debug("Creating wrapper for plugin '{}'", pluginClassName);
		PluginWrapper pluginWrapper = new PluginWrapper(this, pluginDescriptor, null, clazz.getClassLoader());
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
		plugins.put(pluginUuid, pluginWrapper);
		getUnresolvedPlugins().add(pluginWrapper);

		// add plugin class loader to the list with class loaders
		getPluginClassLoaders().put(pluginUuid, clazz.getClassLoader());

		resolvePlugins();

		return pluginWrapper;
	}

	@Override
	public Single<String> deploy(Class<?> clazz, String id) {
		Objects.requireNonNull(id, "A plugin must have a unique id (e.g. hello-world)");
		if (!isMeshPlugin(clazz)) {
			return rxError(BAD_REQUEST, "admin_plugin_error_wrong_type");
		}
		String name = clazz.getSimpleName();
		return Single.defer(() -> {
			log.debug("Deploying plugin class {" + name + "}");
			String uuid;

			// 1. Load plugin
			try {
				uuid = loadPlugin(clazz, id).getPluginId();
			} catch (Throwable e) {
				return Single.error(new RuntimeException("Error while deploying plugin {" + clazz + "}", e));
			}

			// 2. Validate plugin
			try {
				PluginWrapper plugin = getPlugin(uuid);
				if (plugin == null || plugin.getPlugin() == null) {
					log.error("The plugin {" + name + "/" + uuid + "} could not be loaded.");
					plugins.remove(uuid);
					return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
				}
				validate(plugin.getPlugin(), true);
			} catch (GenericRestException e) {
				log.error("Post start validation of plugin {" + name + "/" + uuid + "} failed.", e);
				plugins.remove(uuid);
				return Single.error(e);
			} catch (Throwable e) {
				log.error("Error while loading plugin class", e);
				plugins.remove(uuid);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);
			}

			// 3. Start plugin
			try {
				startPlugin(uuid);
			} catch (Throwable e) {
				log.error("Error while starting plugin", e);
				tryUnloadPlugin(uuid);
				return rxError(INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_starting_failed", name);
			}
			return Single.just(uuid);
		});

	}

	private boolean isMeshPlugin(Class<?> clazz) {
		return MeshPlugin.class.isAssignableFrom(clazz);
	}

	@Override
	public Path getPluginsRoot() {
		return Paths.get(options.getPluginDirectory());
	}

	@Override
	public Set<String> getPluginUuids() {
		return super.getPlugins().stream().map(e -> e.getPluginId()).collect(Collectors.toSet());
	}

	@Override
	public Map<String, String> pluginIdsMap() {
		return getPluginsMap().entrySet()
			.stream()
			.filter(e -> e.getValue() != null)
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
	}

}
