package com.gentics.mesh.verticle;

import io.reactivex.Completable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;
import io.vertx.reactivex.RxHelper;

/**
 * Basic implementation for a job verticle. These kinds of verticles can be used to process specific tasks in a modular fashion. Jobs can be triggered via a
 * specified eventbus address. Each job action is executed synchronously thus a global lock needs to be acquired for each action.
 * 
 * Although the jobs are running in a worker pool thread, the verticle itself is not a worker verticle (any more), because if it were, this would cause eventbus messages,
 * which are published during the job execution (e.g. "node.updated" during a node migration) to not call the handlers immediately (e.g. causing the updated node to be indexed in ES), but would cause
 * all events to be stored in memory and published after the job execution. This causes both an increased memory consumption and delayed index synchronization.
 */
public abstract class AbstractJobVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(AbstractJobVerticle.class);

	public static final String STATUS_ACCEPTED = "accepted";

	public static final String STATUS_REJECTED = "rejected";

	protected boolean stopped = false;

	protected MessageConsumer<Object> jobConsumer;

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		stopped = false;
		registerJobHandler();
		super.start();

	}

	private void registerJobHandler() {
		jobConsumer = vertx.eventBus().consumer(getJobAdress(), message -> {
			// execute blocking, because the verticle itself is not a worker verticle (any more)
			vertx.executeBlocking(prom -> {
				invokeJobAction(message);
			}, rh -> {
				if (rh.failed()) {
					log.error(message);
				}
			});
		});
	}

	/**
	 * Name of the eventbus address which can be used to trigger the job.
	 * 
	 * @return
	 */
	public abstract String getJobAdress();

	/**
	 * Name of the lock that is used to synchronize execution.
	 * 
	 * @return
	 */
	public abstract String getLockName();

	/**
	 * Action which is being invoked.
	 * 
	 * @param message
	 */
	public void invokeJobAction(Message<Object> message) {
		log.info("Got job processing request. Getting lock to execute the request.");
		Completable job = stopped ? Completable.error(new Throwable("Processing was stopped.")) : executeJob(message);
		executeLocked(job, message);
	}

	/**
	 * Invoke actual job execution code.
	 * 
	 * @param message
	 * @return
	 */
	public abstract Completable executeJob(Message<Object> message);

	@Override
	public void stop() throws Exception {
		stopped = true;
		if (jobConsumer != null) {
			jobConsumer.unregister();
		}
	}

	/**
	 * Acquire a cluster wide exclusive lock. By default the method will try to acquire the lock within 10s. The errorAction is invoked if the lock could not be
	 * acquired by then.
	 * 
	 * @param action
	 *            Completable action which will be invoked when the lock has been obtained
	 * @param message
	 */
	protected void executeLocked(Completable action, Message<Object> message) {
		String lockName = getLockName();
		try {
			vertx.sharedData().getLockWithTimeout(lockName, 1000, rh -> {
				if (rh.failed()) {
					Throwable cause = rh.cause();
					log.error("Error while acquiring global lock {" + lockName + "}", cause);
					if (message != null) {
						message.reply(new JsonObject().put("status", STATUS_REJECTED));
					}
				} else {
					Lock lock = rh.result();
					if (message != null) {
						message.reply(new JsonObject().put("status", STATUS_ACCEPTED));
					}
					action.doOnDispose(() -> {
						log.debug("Releasing lock {" + lockName + "}");
						lock.release();
					}).doFinally(() -> {
						log.debug("Releasing lock {" + lockName + "}");
						lock.release();
					}).subscribeOn(RxHelper.blockingScheduler(vertx)).subscribe(() -> {
						log.debug("Action completed");
					}, error -> {
						log.error("Error while executing locked action", error);
					});
				}
			});
		} catch (Exception e) {
			log.error("Error while waiting for global lock {" + lockName + "}", e);
		}
	}

}
