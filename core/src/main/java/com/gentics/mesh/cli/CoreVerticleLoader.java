package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.verticle.ElasticsearchSyncVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
	public RestAPIVerticle restVerticle;

	@Inject
	public JobWorkerVerticle jobWorkerVerticle;

	@Inject
	public ElasticsearchSyncVerticle indexSyncVerticle;

	@Inject
	public MeshOptions meshOptions;

	@Inject
	public CoreVerticleLoader() {

	}

	/**
	 * Load verticles that are configured within the mesh configuration.
	 * 
	 * @param configuration
	 */
	public void loadVerticles(MeshOptions configuration) {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", configuration.getHttpServerOptions().getPort());
		defaultConfig.put("host", configuration.getHttpServerOptions().getHost());
		for (AbstractVerticle verticle : getMandatoryVerticleClasses()) {
			try {
				for (int i = 0; i < DEFAULT_VERTICLE_DEPLOYMENTS; i++) {
					if (log.isInfoEnabled()) {
						log.info("Deploying mandatory verticle {" + verticle.getClass().getName() + "} " + i + " of " + DEFAULT_VERTICLE_DEPLOYMENTS
							+ " instances");
					}
					deployAndWait(Mesh.vertx(), defaultConfig, verticle, false);
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
				deployAndWait(Mesh.vertx(), defaultConfig, verticle, true);
			} catch (Exception e) {
				log.error("Could not load mandatory verticle {" + verticle.getClass().getSimpleName() + "}.", e);
			}
		}
	}

	/**
	 * Return a Map of mandatory verticles.
	 * 
	 * @return
	 */
	private List<AbstractVerticle> getMandatoryVerticleClasses() {
		List<AbstractVerticle> verticles = new ArrayList<>();
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
		// Only deploy search sync verticle if we actually have a configured ES
		ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
		if (searchOptions != null && searchOptions.getUrl() != null) {
			verticles.add(indexSyncVerticle);
		}
		return verticles;
	}

}
