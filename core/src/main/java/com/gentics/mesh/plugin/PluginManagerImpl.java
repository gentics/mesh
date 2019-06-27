package com.gentics.mesh.plugin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.pf4j.DefaultPluginManager;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.PluginRouter;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * @see PluginManager
 */
public class PluginManagerImpl implements PluginManager {

	private static final Logger log = LoggerFactory.getLogger(PluginManagerImpl.class);

	private static Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());

	private static org.pf4j.PluginManager pluginManager = new DefaultPluginManager();

	public PluginManagerImpl() {
	}

	@Override
	public void init(MeshOptions options) {
		String base = options.getPluginDirectory();
		if (base == null) {
			base = "plugins";
		}
		System.setProperty("pf4j.pluginsDir", base);
	}

	@Override
	public Completable deployExistingPluginFiles() {
		return Completable.defer(() -> {
			try {
				pluginManager.loadPlugins();
			} catch (Throwable e) {
				return Completable.error(new RuntimeException("Error while loading plugins.", e));
			}
			try {
				pluginManager.startPlugins();
			} catch (Throwable e) {
				return Completable.error(new RuntimeException("Error while starting plugins.", e));
			}
			return Completable.complete();
		});

	}

	@Override
	public Single<String> deploy(String deploymentName) {
		// DeploymentOptions options = new DeploymentOptions();
		// No longer supported in java 9
		// options.setIsolationGroup(deploymentName);
		return Single.error(new RuntimeException("Not implemented"));
		// return applyRollbackChecks(Mesh.rxVertx()
		// .rxDeployVerticle(deploymentName, options), deploymentName);
	}

	@Override
	public Single<String> deploy(MeshPlugin plugin) {
		return Single.error(new RuntimeException("Not supported"));
	}

	@Override
	public Single<String> deploy(File file) {
		log.debug("Deploying file  {" + file + "}");
		String id;
		try {
			id = pluginManager.loadPlugin(file.toPath());
		} catch (Throwable e) {
			return Single.error(new RuntimeException("Error while deploying plugin file {" + file + "}", e));
		}
		try {
			pluginManager.startPlugin(id);
		} catch (Throwable e) {
			return Single.error(new RuntimeException("Error while starting plugin file {" + file + "/" + id + "}", e));
		}
		return Single.just(id);
	}

	// /**
	// * Apply checks which will undeploy the verticle if it did not register itself.
	// *
	// * @param deploy
	// * @param name
	// * @return Modified single which contains the rollback handlers.
	// */
	// private Single<String> applyRollbackChecks(Single<String> deploy, String name) {
	// return deploy.onErrorResumeNext(error -> {
	// if (error instanceof GenericRestException) {
	// // Catch and throw errors which might occurred during plugin startup/validation.
	// return Single.error(error);
	// } else {
	// log.error("Plugin deployment of {" + name + "} failed.", error);
	// return Single.error(error(BAD_REQUEST, "admin_plugin_error_plugin_deployment_failed", name));
	// }
	// })
	// .map(UUIDUtil::toShortUuid)
	// .flatMap(uuid -> {
	// if (!deployments.containsKey(uuid)) {
	// log.warn("The plugin was not registered after deployment. Maybe the initialisation failed. Going to undeploy the plugin.");
	// GenericRestException exception = error(BAD_REQUEST, "admin_plugin_error_plugin_did_not_register");
	// return undeploy(uuid).andThen(Single.error(exception));
	// } else {
	// return Single.just(uuid);
	// }
	// });
	// }

	@Override
	public Completable undeploy(String uuid) {
		return Mesh.rxVertx().rxUndeploy(UUIDUtil.toFullUuid(uuid));
	}

	@Override
	public Completable validate(MeshPlugin plugin) {
		return Completable.create(sub -> {
			Objects.requireNonNull(plugin, "The plugin must not be null");
			checkForConflict(plugin);
			plugin.getManifest().validate();
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
	public Completable registerPlugin(MeshPlugin plugin) {
		Objects.requireNonNull(plugin, "The plugin must not be null");
		return validate(plugin)
			.andThen(plugin.initialize())
			.andThen(Completable.create(sub -> {
				String name = plugin.getName();
				String apiName = plugin.getManifest().getApiName();
				log.info("Registering plugin {" + name + "} with id {" + plugin.deploymentID() + "}");
				for (RouterStorage rs : RouterStorage.getInstances()) {
					PluginRouter globalPluginRouter = rs.root().apiRouter().pluginRouter();
					PluginRouter projectPluginRouter = rs.root().apiRouter().projectsRouter().projectRouter().pluginRouter();

					Router globalRouter = globalPluginRouter.getRouter(apiName);
					Router projectRouter = projectPluginRouter.getRouter(apiName);
					plugin.registerEndpoints(globalRouter, projectRouter);
				}
				// deployments.put(plugin.deploymentID(), plugin);
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

	@Override
	public Completable deregisterPlugin(MeshPlugin plugin) {
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
	public MeshPlugin getPlugin(String uuid) {
		return (MeshPlugin) pluginManager.getPlugin(uuid).getPlugin();
	}

	@Override
	public Map<String, MeshPlugin> getPlugins() {
		// return pluginManager.getPlugins().stream().collect(Collectors.toMap(e -> e, valueMapper));
		return null;
	}

	@Override
	public Completable stop() {
		return Completable.fromRunnable(() -> {
			pluginManager.stopPlugins();
		});
	}

	@Override
	public String adminToken() {
		MeshComponent mesh = MeshInternal.get();
		MeshJWTAuthProvider authProvider = mesh.authProvider();
		Database db = mesh.database();

		return db.tx(() -> {
			User admin = mesh.boot().userRoot().findByUsername("admin");
			// TODO: Use dedicated tokenCode - See https://github.com/gentics/mesh/issues/412
			return authProvider.generateAPIToken(admin, null, null);
		});
	}

}
