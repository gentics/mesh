package com.gentics.mesh.core.verticle.job;

import static com.gentics.mesh.Events.JOB_WORKER_ADDRESS;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Dedicated verticle which will process jobs.
 */
@Singleton
public class JobWorkerVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(JobWorkerVerticle.class);

	private static final String GLOBAL_JOB_LOCK_NAME = "mesh.internal.joblock";

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	private MessageConsumer<Object> jobConsumer;

	private Lazy<BootstrapInitializer> boot;

	private Database db;

	private Long periodicTimerId;

	private long timerId;

	private boolean stopped = false;

	@Inject
	public JobWorkerVerticle(Database db, Lazy<BootstrapInitializer> boot) {
		this.db = db;
		this.boot = boot;
	}

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		stopped = false;
		registerJobHandler();

		// The verticle has been deployed. Now wait a few seconds and schedule the periodic execution of jobs
		timerId = vertx.setTimer(30_000, rh -> {
			periodicTimerId = vertx.setPeriodic(15_000, th -> {
				processJobs();
			});
		});

		super.start();
	}

	private void registerJobHandler() {
		jobConsumer = vertx.eventBus().consumer(JOB_WORKER_ADDRESS, (message) -> {
			log.info("Got job processing request. Getting lock to execute the request.");
			processJobs();
		});
	}

	private void processJobs() {
		executeLocked(() -> {
			if (!stopped) {
				db.tx(() -> {
					JobRoot jobRoot = boot.get().jobRoot();
					jobRoot.process();
				});
			}
		}, error -> {
			log.error("Error while processing jobs", error);
		});
	}

	@Override
	public void stop() throws Exception {
		if (jobConsumer != null) {
			jobConsumer.unregister();
		}
		vertx.cancelTimer(timerId);
		if (periodicTimerId != null) {
			vertx.cancelTimer(periodicTimerId);
		}
		stopped = true;
		super.stop();
	}

	/**
	 * Acquire a cluster wide exclusive lock. By default the method will try to acquire the lock within 10s. The errorAction is invoked if the lock could not be
	 * acquired by then.
	 * 
	 * @param action
	 *            Action which will be invoked when the lock has been obtained
	 * @param errorAction
	 *            Action which will be invoked when the lock could not be obtained or the action failed.
	 */
	protected void executeLocked(Action0 action, Action1<Throwable> errorAction) {
		try {
			vertx.sharedData().getLock(GLOBAL_JOB_LOCK_NAME, rh -> {
				if (rh.failed()) {
					Throwable cause = rh.cause();
					log.error("Error while acquiring global migration lock {" + GLOBAL_JOB_LOCK_NAME + "}", cause);
					errorAction.call(cause);
				} else {
					Lock lock = rh.result();
					try {
						action.call();
					} catch (Exception e) {
						log.error("Error while executing locked action", e);
						errorAction.call(e);
					} finally {
						lock.release();
					}
				}
			});
		} catch (Exception e) {
			log.error("Error while waiting for global lock {" + GLOBAL_JOB_LOCK_NAME + "}", e);
			errorAction.call(e);
		}
	}

}
