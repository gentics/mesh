package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.RestAPIVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class CoreVerticleLoader {

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	@Inject
	public RestAPIVerticle restVerticle;

	@Inject
	public JobWorkerVerticle jobWorkerVerticle;

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
				if (log.isInfoEnabled()) {
					log.info("Loading mandatory verticle {" + verticle.getClass().getName() + "}.");
				}
				deployAndWait(Mesh.vertx(), defaultConfig, verticle, false);
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
	 * Get the map of mandatory worker verticle classes
	 * 
	 * @return
	 */
	private List<AbstractVerticle> getMandatoryWorkerVerticleClasses() {
		List<AbstractVerticle> verticles = new ArrayList<>();
		verticles.add(jobWorkerVerticle);
		return verticles;
	}

}
