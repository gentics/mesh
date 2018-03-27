package com.gentics.mesh.verticle;

import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;

/**
 * Basic implementation for a job verticle. These kinds of verticles can be used to process specific tasks in a modular fashion. Jobs can be triggered via a
 * specified eventbus address. Each job action is executed synchronously thus a global lock needs to be acquired for each action.
 */
public abstract class AbstractJobVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(AbstractJobVerticle.class);

	protected boolean stopped = false;

	protected MessageConsumer<Object> jobConsumer;

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		registerJobHandler();
		super.start();

	}

	private void registerJobHandler() {
		jobConsumer = vertx.eventBus().consumer(getJobAdress(), (message) -> {
			invokeJobAction(message);
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
		executeLocked(() -> {
			if (!stopped) {
				executeJob(message);
			}
		}, error -> {
			log.error("Error while processing jobs", error);
		});
	}

	public abstract void executeJob(Message<Object> message);

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
	 *            Action which will be invoked when the lock has been obtained
	 * @param errorAction
	 *            Action which will be invoked when the lock could not be obtained or the action failed.
	 */
	protected void executeLocked(Runnable action, Consumer<Throwable> errorAction) {
		String lockName = getLockName();
		try {
			vertx.sharedData().getLock(lockName, rh -> {
				if (rh.failed()) {
					Throwable cause = rh.cause();
					log.error("Error while acquiring global lock {" + lockName + "}", cause);
					errorAction.accept(cause);
				} else {
					Lock lock = rh.result();
					try {
						action.run();
					} catch (Exception e) {
						log.error("Error while executing locked action", e);
						errorAction.accept(e);
					} finally {
						lock.release();
					}
				}
			});
		} catch (Exception e) {
			log.error("Error while waiting for global lock {" + lockName + "}", e);
			errorAction.accept(e);
		}
	}

}
