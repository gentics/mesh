package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.pub.PublicAPIVerticle;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.verticle.ElasticsearchSyncVerticle;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

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
	public Provider<PublicAPIVerticle> publicAPIVerticle;

	@Inject
	public JobWorkerVerticle jobWorkerVerticle;

	@Inject
	public ElasticsearchSyncVerticle indexSyncVerticle;

	@Inject
	public MeshOptions meshOptions;

	private final Vertx rxVertx;

	@Inject
	public CoreVerticleLoader(Vertx rxVertx) {
		this.rxVertx = rxVertx;
	}

	private final List<String> deploymentIds = new ArrayList<>();

	/**
	 * Load verticles that are configured within the mesh configuration.
	 */
	public void loadVerticles() {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", meshOptions.getHttpServerOptions().getPort());
		defaultConfig.put("host", meshOptions.getHttpServerOptions().getHost());
		for (Provider<? extends AbstractVerticle> verticle : getMandatoryVerticleClasses()) {
			try {
				for (int i = 0; i < DEFAULT_VERTICLE_DEPLOYMENTS; i++) {
					if (log.isInfoEnabled()) {
						log.info("Deploying mandatory verticle {" + verticle.getClass().getName() + "} " + i + " of " + DEFAULT_VERTICLE_DEPLOYMENTS
							+ " instances");
					}
					deploymentIds.add(deployAndWait(rxVertx.getDelegate(), defaultConfig, verticle.get(), false));
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
				deploymentIds.add(deployAndWait(rxVertx.getDelegate(), defaultConfig, verticle, true));
			} catch (Exception e) {
				log.error("Could not load mandatory verticle {" + verticle.getClass().getSimpleName() + "}.", e);
			}
		}
	}

	public Completable unloadVerticles() {
		return Observable.fromIterable(deploymentIds)
			.flatMapCompletable(rxVertx::rxUndeploy)
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
		verticles.add(publicAPIVerticle);
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
		// Only deploy search sync verticle if we actually have a configured ES
		ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
		if (searchOptions != null && searchOptions.getUrl() != null) {
			verticles.add(indexSyncVerticle);
		}
		return verticles;
	}

}
