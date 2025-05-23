package com.gentics.mesh.cli;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.core.verticle.job.JobWorkerVerticleImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.monitor.MonitoringServerVerticle;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.util.RxUtil;

import com.gentics.mesh.verticle.BinaryCheckVerticle;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Central loader for core verticles. Needed verticles will be listed and deployed here.
 */
@Singleton
public class CoreVerticleLoader {

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	protected final Provider<RestAPIVerticle> restVerticle;

	protected final Provider<MonitoringServerVerticle> monitoringServerVerticle;

	protected final JobWorkerVerticleImpl jobWorkerVerticle;

	protected final BinaryCheckVerticle binaryCheckVerticle;

	protected final Provider<ElasticsearchProcessVerticle> elasticsearchProcessVerticleProvider;
	private ElasticsearchProcessVerticle elasticsearchProcessVerticle;

	protected final MeshOptions meshOptions;

	private final Vertx rxVertx;
	private String searchVerticleId;

	@Inject
	public CoreVerticleLoader(Vertx rxVertx, Provider<RestAPIVerticle> restVerticle,
			Provider<MonitoringServerVerticle> monitoringServerVerticle, JobWorkerVerticleImpl jobWorkerVerticle,
			BinaryCheckVerticle binaryCheckVerticle, Provider<ElasticsearchProcessVerticle> elasticsearchProcessVerticleProvider,
			MeshOptions meshOptions) {
		this.rxVertx = rxVertx;
		this.restVerticle = restVerticle;
		this.monitoringServerVerticle = monitoringServerVerticle;
		this.jobWorkerVerticle = jobWorkerVerticle;
		this.binaryCheckVerticle = binaryCheckVerticle;
		this.elasticsearchProcessVerticleProvider = elasticsearchProcessVerticleProvider;
		this.meshOptions = meshOptions;
	}

	private JsonObject defaultConfig;

	/**
	 * Load verticles that are configured within the mesh configuration.
	 *
	 * @param initialProjects
	 */
	public Completable loadVerticles(List<String> initialProjects) {
		defaultConfig = new JsonObject();
		defaultConfig.put("port", meshOptions.getHttpServerOptions().getPort());
		defaultConfig.put("host", meshOptions.getHttpServerOptions().getHost());
		defaultConfig.put("initialProjects", initialProjects);

		return Completable.mergeArray(
			deployRestVerticle(),
			deployMonitoringVerticle(),
			deployJobWorkerVerticle(),
			deployBinaryCheckVerticle(),
			deploySearchVerticle());
	}

	private Completable deployRestVerticle() {
		return rxVertx.rxDeployVerticle(restVerticle::get, new DeploymentOptions()
			.setConfig(defaultConfig)
			.setInstances(meshOptions.getHttpServerOptions().getVerticleAmount()))
			.ignoreElement();
	}

	private Completable deployMonitoringVerticle() {
		if (meshOptions.getMonitoringOptions() != null && meshOptions.getMonitoringOptions().isEnabled()) {
			return rxVertx.rxDeployVerticle(monitoringServerVerticle::get, new DeploymentOptions()
				.setInstances(1))
				.ignoreElement();
		} else {
			return Completable.complete();
		}
	}

	private Completable deployJobWorkerVerticle() {
		// the verticle is not deployed as worker verticle (any more). See comments of AbstractJobVerticle for details
		return rxVertx.rxDeployVerticle(jobWorkerVerticle, new DeploymentOptions()
			.setInstances(1)
			.setWorker(false))
			.ignoreElement();
	}

	private Completable deployBinaryCheckVerticle() {
		return rxVertx.rxDeployVerticle(binaryCheckVerticle, new DeploymentOptions().setInstances(1)).ignoreElement();
	}

	private Completable deploySearchVerticle() {
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

	/**
	 * Undeploy search verticle
	 *
	 * @return
	 */
	public Completable undeploySearchVerticle() {
		return RxUtil.fromNullable(searchVerticleId)
			.flatMapCompletable(rxVertx::rxUndeploy)
			.andThen(resetSearchVerticle());
	}

	/**
	 * Redeploy the search verticle, if it is not deployed
	 * @return completable
	 */
	public Completable redeploySearchVerticle() {
		if (searchVerticleId == null) {
			return deploySearchVerticle();
		} else {
			return Completable.complete();
		}
	}

	private Completable resetSearchVerticle() {
		searchVerticleId = null;
		return Completable.complete();
	}

	public ElasticsearchProcessVerticle getSearchVerticle() {
		return elasticsearchProcessVerticle;
	}
}
