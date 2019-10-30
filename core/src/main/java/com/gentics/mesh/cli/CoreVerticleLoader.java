package com.gentics.mesh.cli;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.monitor.MonitoringServerVerticle;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Maybe;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Central loader for core verticles. Needed verticles will be listed and deployed here.
 */
@Singleton
public class CoreVerticleLoader {

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	@Inject
	public Provider<RestAPIVerticle> restVerticle;

	@Inject
	public Provider<MonitoringServerVerticle> monitoringServerVerticle;

	@Inject
	public JobWorkerVerticle jobWorkerVerticle;

	@Inject
	public ElasticsearchProcessVerticle elasticsearchProcessVerticle;

	@Inject
	public MeshOptions meshOptions;

	private final Vertx rxVertx;

	private String searchVerticleId;

	@Inject
	public CoreVerticleLoader(Vertx rxVertx) {
		this.rxVertx = rxVertx;
	}

	private JsonObject defaultConfig;

	/**
	 * Load verticles that are configured within the mesh configuration.
	 * 
	 * @param initialProjects
	 */
	public void loadVerticles(List<String> initialProjects) {
		defaultConfig = new JsonObject();
		defaultConfig.put("port", meshOptions.getHttpServerOptions().getPort());
		defaultConfig.put("host", meshOptions.getHttpServerOptions().getHost());
		defaultConfig.put("initialProjects", initialProjects);

		deployRestVerticle();
		deployMonitoringVerticle();
		deployJobWorkerVerticle();
		deploySearchVerticle();
	}

	private void deployRestVerticle() {
		rxVertx.deployVerticle(restVerticle::get, new DeploymentOptions()
			.setConfig(defaultConfig)
			.setInstances(meshOptions.getHttpServerOptions().getVerticleAmount()));
	}

	private void deployMonitoringVerticle() {
		if (meshOptions.getMonitoringOptions() != null && meshOptions.getMonitoringOptions().isEnabled()) {
			rxVertx.deployVerticle(monitoringServerVerticle::get, new DeploymentOptions()
				.setInstances(1));
		}
	}

	private void deployJobWorkerVerticle() {
		rxVertx.deployVerticle(jobWorkerVerticle, new DeploymentOptions()
			.setInstances(1)
			.setWorker(true));
	}

	private void deploySearchVerticle() {
		// Only deploy search sync verticle if we actually have a configured ES
		ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
		if (searchOptions != null && searchOptions.getUrl() != null) {
			rxVertx.rxDeployVerticle(elasticsearchProcessVerticle, new DeploymentOptions()
				.setInstances(1))
				.subscribe(id -> searchVerticleId = id);
		}
	}

	public void reloadSearchVerticle() {
		RxUtil.fromNullable(searchVerticleId)
			.flatMapCompletable(rxVertx::rxUndeploy)
			.subscribe(this::deploySearchVerticle);
	}

	public ElasticsearchProcessVerticle getSearchVerticle() {
		return elasticsearchProcessVerticle;
	}
}
