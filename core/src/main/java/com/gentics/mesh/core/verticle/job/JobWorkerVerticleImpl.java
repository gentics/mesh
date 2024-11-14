package com.gentics.mesh.core.verticle.job;

import static com.gentics.mesh.core.rest.MeshEvent.JOB_WORKER_ADDRESS;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.jobs.JobProcessor;
import com.gentics.mesh.distributed.MasterInfoProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.verticle.AbstractJobVerticle;

import dagger.Lazy;
import io.reactivex.Completable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.shareddata.Lock;

/**
 * Dedicated verticle which will process jobs.
 */
@Singleton
public class JobWorkerVerticleImpl extends AbstractJobVerticle implements JobWorkerVerticle {

	private static final String GLOBAL_JOB_LOCK_NAME = "mesh.internal.joblock";

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String BRANCH_UUID_HEADER = "branchUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	private Lazy<BootstrapInitializer> boot;
	private JobProcessor jobProcessor;
	private Database db;
	private final MasterInfoProvider masterInfoProvider;
	private final boolean clusteringEnabled;

	@Inject
	public JobWorkerVerticleImpl(Database db, Lazy<BootstrapInitializer> boot, JobProcessor jobProcessor,
			MeshOptions options, MasterInfoProvider masterInfoProvider) {
		this.db = db;
		this.boot = boot;
		this.jobProcessor = jobProcessor;
		this.masterInfoProvider = masterInfoProvider;
		this.clusteringEnabled = options.getClusterOptions().isEnabled();
	}

	@Override
	public void start() throws Exception {
		super.start();

		long migrationTriggerInterval = boot.get().mesh().getOptions().getMigrationTriggerInterval();

		if (migrationTriggerInterval > 0) {
			vertx.setPeriodic(migrationTriggerInterval, id -> {
				if (!isCurrentMaster()) {
					log.debug("Not invoking job processing, because instance is not the current master");
				} else if (jobProcessor.isProcessing()) {
					log.debug("Not invoking job processing, because jobs are currently processed");
				} else {
					log.debug("Invoke job processing");
					vertx.eventBus().publish(getJobAdress(), null);
				}
			});
		}
	}

	@Override
	public String getJobAdress() {
		return JOB_WORKER_ADDRESS + boot.get().mesh().getOptions().getNodeName();
	}

	@Override
	public String getLockName() {
		return GLOBAL_JOB_LOCK_NAME;
	}

	@Override
	public Completable executeJob(Message<Object> message) {
		return Completable.defer(() -> jobProcessor.process());
	}

	@Override
	public void doWithLock(long timeout, Handler<AsyncResult<Lock>> resultHandler) {
		vertx.sharedData().getLockWithTimeout(getLockName(), timeout, resultHandler);
	}

	/**
	 * Check whether the instance is currently the master
	 * @return true for the master (or clustering not enabled)
	 */
	private boolean isCurrentMaster() {
		if (clusteringEnabled) {
			return masterInfoProvider.isMaster();
		} else {
			return true;
		}
	}
}
