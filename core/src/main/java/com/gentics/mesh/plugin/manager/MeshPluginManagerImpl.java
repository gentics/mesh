package com.gentics.mesh.plugin.manager;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

	private final MeshOptions options;

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
		return validate(plugin, strict)
			.andThen(plugin.initialize())
			.andThen(Completable.create(sub -> {
				if (plugin instanceof RestPlugin) {
					RestPlugin restPlugin = ((RestPlugin) plugin);
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
				syncSet.remove(plugin.getManifest().getApiName());
			});
	}

	private Completable deregisterPlugin(MeshPlugin plugin) {
		return Completable.create(sub -> {
			String name = plugin.getName();
			log.info("Deregistering {" + name + "} plugin.");

			// deployments.remove(plugin.deploymentID());

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
	public void init(MeshOptions options) {
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
		log.debug("Deploying file {" + path + "}");
		String id;
		try {
			id = loadPlugin(path);
		} catch (Throwable e) {
			return Single.error(new RuntimeException("Error while deploying plugin file {" + path + "}", e));
		}
		try {
			startPlugin(id);
		} catch (Throwable e) {
			return Single.error(new RuntimeException("Error while starting plugin file {" + path + "/" + id + "}", e));
		}
		return Single.just(id);
	}

	// log.error("Plugin deployment of {" + name + "} failed.", error);
	// return Single.error(error(BAD_REQUEST, "admin_plugin_error_plugin_deployment_failed", name));
	// log.warn("The plugin was not registered after deployment. Maybe the initialisation failed. Going to undeploy the plugin.");
	// GenericRestException exception = error(BAD_REQUEST, "admin_plugin_error_plugin_did_not_register");

	@Override
	public Completable undeploy(String uuid) {
		return Completable.fromRunnable(() -> {
			resolvePlugins();
			unloadPlugin(uuid);
		});
	}

	@Override
	public Completable validate(MeshPlugin plugin, boolean strict) {
		return Completable.create(sub -> {
			Objects.requireNonNull(plugin, "The plugin must not be null");
			checkForConflict(plugin);
			PluginUtils.validate(plugin.getManifest(), strict);
			PluginUtils.validate(plugin);
			sub.onComplete();
		});
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
		} else {
			syncSet.add(apiName);
		}
	}

	@Override
	public Map<String, MeshPlugin> getPluginsMap() {
		return getPlugins().stream().collect(Collectors.toMap(PluginWrapper::getPluginId, e -> (MeshPlugin) e.getPlugin()));
	}

	@Override
	public Completable stop() {
		return Completable.fromRunnable(() -> {
			stopPlugins();
		});
	}

	@Override
	public void unload() {
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

	private PluginWrapper loadPlugin(Class<?> clazz, String pluginId) {
		MeshPluginDescriptor pluginDescriptor = new MeshPluginDescriptorImpl(pluginId, clazz);

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
		plugins.put(pluginId, pluginWrapper);
		getUnresolvedPlugins().add(pluginWrapper);

		// add plugin class loader to the list with class loaders
		getPluginClassLoaders().put(pluginId, clazz.getClassLoader());

		resolvePlugins();

		return pluginWrapper;
	}

	@Override
	public Single<String> deploy(Class<?> clazz, String pluginId) {
		// if (!(clazz.isAssignableFrom(MeshPlugin.class))) {
		// throw new RuntimeException("The plugin is not a Mesh Plugin. Only mesh plugins are deployable.");
		// }
		return Single.defer(() -> {
			log.debug("Deploying plugin class {" + clazz.getName() + "}");
			try {
				loadPlugin(clazz, pluginId);
			} catch (Throwable e) {
				return Single.error(new RuntimeException("Error while deploying plugin file {" + clazz + "}", e));
			}
			try {
				startPlugin(pluginId);
			} catch (Throwable e) {
				log.error("Error while starting plugin", e);
				try {
					unloadPlugin(pluginId);
				} catch (Exception e2) {
					log.error("Error while unloading the plugin {" + pluginId + "}");
				}
				return Single.error(new RuntimeException("Error while starting plugin file {" + clazz + "/" + pluginId + "}", e));
			}
			return Single.just(pluginId);
		});

	}

	@Override
	public Path getPluginsRoot() {
		return Paths.get(options.getPluginDirectory());
	}

	@Override
	public Set<String> getPluginIds() {
		return super.getPlugins().stream().map(e -> e.getPluginId()).collect(Collectors.toSet());
	}

}
