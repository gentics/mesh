package com.gentics.mesh.core.verticle.migration.release;

import static com.gentics.mesh.Events.RELEASE_MIGRATION_ADDRESS;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.admin.MigrationType;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationVerticle;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.verticle.migration.node.NodeMigrationHandler;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import groovy.lang.Singleton;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class ReleaseMigrationVerticle extends AbstractMigrationVerticle<NodeMigrationHandler> {

	private static final Logger log = LoggerFactory.getLogger(ReleaseMigrationVerticle.class);

	private MessageConsumer<Object> releaseMigrationConsumer;

	@Inject
	public ReleaseMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot, NodeMigrationHandler nodeMigrationHandler) {
		super(nodeMigrationHandler, db, boot);
	}

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		registerReleaseMigration();
		super.start();
	}

	@Override
	public void stop() throws Exception {
		if (releaseMigrationConsumer != null) {
			releaseMigrationConsumer.unregister();
		}
		super.stop();
	}

	/**
	 * Register handler for release migration events.
	 */
	private void registerReleaseMigration() {
		releaseMigrationConsumer = vertx.eventBus().consumer(RELEASE_MIGRATION_ADDRESS, (message) -> {
			MigrationStatusHandler status = new MigrationStatusHandlerImpl(message, vertx, MigrationType.release);
			try {
				String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
				String releaseUuid = message.headers().get(UUID_HEADER);

				if (log.isDebugEnabled()) {
					log.debug("Release migration for release {" + releaseUuid + "} was requested");
				}

				db.tx(() -> {
					Project project = boot.get().projectRoot().findByUuid(projectUuid);
					if (project == null) {
						throw error(BAD_REQUEST, "Project with uuid {" + projectUuid + "} cannot be found.");
					}

					Release release = project.getReleaseRoot().findByUuid(releaseUuid);
					if (release == null) {
						throw error(BAD_REQUEST, "Release with uuid {" + releaseUuid + "} cannot be found.");
					}

					// Acquire the global lock and invoke the migration
					executeLocked(() -> {
						db.tx(() -> {
							handler.migrateNodes(release).await();
						});
						status.done();
					}, (error) -> {
						status.handleError(error, "Release migration for release { " + releaseUuid + "} failed.");
					});
				});
			} catch (Exception e) {
				status.handleError(e, "Error while preparing release migration.");
			}
		});
	}

}
