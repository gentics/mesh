package com.gentics.mesh.cli;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.search.verticle.eventhandler.SyncHandler;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

/**
 * Central loader for core verticles. Needed verticles will be listed and deployed here.
 */
@Singleton
public class CoreVerticleLoader {

	/**
	 * Default amount of verticle instances which should be deployed.
	 */
	private static final int DEFAULT_VERTICLE_DEPLOYMENTS = 5;

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	@Inject
	public Provider<RestAPIVerticle> restVerticle;

	@Inject
	public JobWorkerVerticle jobWorkerVerticle;

	@Inject
	public Provider<SyncHandler> indexSyncVerticle;

	@Inject
	public Lazy<ElasticsearchProcessVerticle> elasticsearchProcessVerticle;

	@Inject
	public MeshOptions configuration;

	@Inject
	public CoreVerticleLoader() {

	}

	private final List<String> deploymentIds = new ArrayList<>();

	/**
	 * Load verticles that are configured within the mesh configuration.
	 */
	public void loadVerticles() {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", configuration.getHttpServerOptions().getPort());
		defaultConfig.put("host", configuration.getHttpServerOptions().getHost());
		for (Provider<? extends AbstractVerticle> verticle : getMandatoryVerticleClasses()) {
			try {
				for (int i = 0; i < DEFAULT_VERTICLE_DEPLOYMENTS; i++) {
					if (log.isInfoEnabled()) {
						log.info("Deploying mandatory verticle {" + verticle.getClass().getName() + "} " + i + " of " + DEFAULT_VERTICLE_DEPLOYMENTS
							+ " instances");
					}
					deploymentIds.add(deployAndWait(Mesh.vertx(), defaultConfig, verticle.get(), false));
				}
			} catch (Exception e) {
				log.error("Could not load mandatory verticle {" + verticle.getClass().getSimpleName() + "}.", e);
			}
		}

		for (AbstractVerticle verticle : getMandatoryWorkerVerticleClasses()) {
			try {
				if (log.isInfoEnabled()) {
					log.info("Loading mandatory verticle {" + verticle.getClass().getName() + "}.");
				}
				deploymentIds.add(deployAndWait(Mesh.vertx(), defaultConfig, verticle, true));
			} catch (Exception e) {
				log.error("Could not load mandatory verticle {" + verticle.getClass().getSimpleName() + "}.", e);
			}
		}
	}

	public Completable unloadVerticles() {
		return Observable.fromIterable(deploymentIds)
			.flatMapCompletable(Mesh.rxVertx()::rxUndeploy)
			.doOnComplete(deploymentIds::clear);
	}

	/**
	 * Return a Map of mandatory verticles.
	 * 
	 * @return
	 */
	private List<Provider<? extends AbstractVerticle>> getMandatoryVerticleClasses() {
		List<Provider<? extends AbstractVerticle>> verticles = new ArrayList<>();
		verticles.add(restVerticle);
		return verticles;
	}

	/**
	 * Get the map of mandatory worker verticle classes.
	 * 
	 * @return
	 */
	private List<AbstractVerticle> getMandatoryWorkerVerticleClasses() {
		List<AbstractVerticle> verticles = new ArrayList<>();
		verticles.add(jobWorkerVerticle);
		// TODO Only add verticle if necessary
		verticles.add(elasticsearchProcessVerticle.get());
		return verticles;
	}

}
