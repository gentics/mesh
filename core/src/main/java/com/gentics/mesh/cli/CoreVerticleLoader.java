package com.gentics.mesh.cli;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import com.gentics.mesh.core.verticle.job.JobWorkerVerticleImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.monitor.MonitoringServerVerticle;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Central loader for core verticles. Needed verticles will be listed and deployed here.
 */
public abstract class CoreVerticleLoader implements VerticleLoader {

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	@Inject
	public Provider<RestAPIVerticle> restVerticle;

	@Inject
	public Provider<MonitoringServerVerticle> monitoringServerVerticle;

	@Inject
	public JobWorkerVerticleImpl jobWorkerVerticle;

	@Inject
	public Provider<ElasticsearchProcessVerticle> elasticsearchProcessVerticleProvider;
	private ElasticsearchProcessVerticle elasticsearchProcessVerticle;

	@Inject
	public MeshOptions meshOptions;

	protected final Vertx rxVertx;
	protected String searchVerticleId;

	public CoreVerticleLoader(Vertx rxVertx) {
		this.rxVertx = rxVertx;
	}

	private JsonObject defaultConfig;

	@Override
	public Completable loadVerticles(List<String> initialProjects) {
		defaultConfig = new JsonObject();
		defaultConfig.put("port", meshOptions.getHttpServerOptions().getPort());
		defaultConfig.put("host", meshOptions.getHttpServerOptions().getHost());
		defaultConfig.put("initialProjects", initialProjects);

		return deployAll();
	}

	protected Completable deployAll() {
		return Completable.mergeArray(
				deployRestVerticle(),
				deployMonitoringVerticle(),
				deployJobWorkerVerticle(),
				deploySearchVerticle());
	}

	protected Completable deployRestVerticle() {
		return rxVertx.rxDeployVerticle(restVerticle::get, new DeploymentOptions()
			.setConfig(defaultConfig)
			.setInstances(meshOptions.getHttpServerOptions().getVerticleAmount()))
			.ignoreElement();
	}

	protected Completable deployMonitoringVerticle() {
		if (meshOptions.getMonitoringOptions() != null && meshOptions.getMonitoringOptions().isEnabled()) {
			return rxVertx.rxDeployVerticle(monitoringServerVerticle::get, new DeploymentOptions()
				.setInstances(1))
				.ignoreElement();
		} else {
			return Completable.complete();
		}
	}

	protected Completable deployJobWorkerVerticle() {
		// the verticle is not deployed as worker verticle (any more). See comments of AbstractJobVerticle for details
		return rxVertx.rxDeployVerticle(jobWorkerVerticle, new DeploymentOptions()
			.setInstances(1)
			.setWorker(false))
			.ignoreElement();
	}

	protected Completable deploySearchVerticle() {
		// Only deploy search sync verticle if we actually have a configured ES
		ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
		if (searchOptions != null && searchOptions.getUrl() != null) {
			return Completable.defer(() -> {
				elasticsearchProcessVerticle = elasticsearchProcessVerticleProvider.get();
				return rxVertx.rxDeployVerticle(elasticsearchProcessVerticle, new DeploymentOptions()
					.setInstances(1))
					.doOnSuccess(id -> searchVerticleId = id)
					.ignoreElement();
			});
		} else {
			return Completable.complete();
		}
	}

	@Override
	public Completable redeploySearchVerticle() {
		return RxUtil.fromNullable(searchVerticleId)
			.flatMapCompletable(rxVertx::rxUndeploy)
			.andThen(deploySearchVerticle());
	}

	public ElasticsearchProcessVerticle getSearchVerticle() {
		return elasticsearchProcessVerticle;
	}
}
