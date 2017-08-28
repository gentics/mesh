package com.gentics.mesh.core.verticle.migration;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Abstract implementation of an migration verticle.
 *
 * @param <T>
 */
public abstract class AbstractMigrationVerticle<T extends MigrationHandler> extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationVerticle.class);

	private static final String GLOBAL_MIGRATION_LOCK_NAME = "mesh.global.migrationlock";

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	protected Database db;

	protected Lazy<BootstrapInitializer> boot;

	protected T handler;

	public AbstractMigrationVerticle(T handler, Database db, Lazy<BootstrapInitializer> boot) {
		this.handler = handler;
		this.db = db;
		this.boot = boot;
	}

	/**
	 * Acquire a cluster wide exclusive lock. By default the method will try to acquire the lock within 10s. The errorAction is invoked if the lock could not be
	 * acquired by then.
	 * 
	 * @param action
	 *            Action which will be invoked when the lock has been obtained
	 * @param errorAction
	 *            Action which will be invoked
	 */
	protected void executeLocked(Action0 action, Action1<Throwable> errorAction) {
		try {
			vertx.sharedData().getLock(GLOBAL_MIGRATION_LOCK_NAME, rh -> {
				if (rh.failed()) {
					Throwable cause = rh.cause();
					log.error("Error while acquiring global migration lock", cause);
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
			log.error("Error while waiting for global lock {" + GLOBAL_MIGRATION_LOCK_NAME + "}", e);
			errorAction.call(e);
		}
	}

}
