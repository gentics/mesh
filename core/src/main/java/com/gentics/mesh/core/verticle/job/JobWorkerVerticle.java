package com.gentics.mesh.core.verticle.job;

import static com.gentics.mesh.Events.JOB_WORKER_ADDRESS;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.verticle.AbstractJobVerticle;

import dagger.Lazy;
import io.reactivex.Completable;
import io.vertx.core.eventbus.Message;

/**
 * Dedicated verticle which will process jobs.
 */
@Singleton
public class JobWorkerVerticle extends AbstractJobVerticle {

	private static final String GLOBAL_JOB_LOCK_NAME = "mesh.internal.joblock";

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	private Lazy<BootstrapInitializer> boot;

	private Database db;

	@Inject
	public JobWorkerVerticle(Database db, Lazy<BootstrapInitializer> boot) {
		this.db = db;
		this.boot = boot;
	}

	@Override
	public String getJobAdress() {
		return JOB_WORKER_ADDRESS;
	}

	@Override
	public String getLockName() {
		return GLOBAL_JOB_LOCK_NAME;
	}

	@Override
	public Completable executeJob(Message<Object> message) {
		return Completable.fromAction(() -> {
			db.tx(() -> {
				JobRoot jobRoot = boot.get().jobRoot();
				jobRoot.process();
			});
		});
	}

}
