package com.gentics.mesh.core.verticle.migration.release;

import static com.gentics.mesh.Events.MESH_MIGRATION;
import static com.gentics.mesh.Events.RELEASE_MIGRATION_ADDRESS;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationVerticle;
import com.gentics.mesh.core.verticle.migration.node.NodeMigrationHandler;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import groovy.lang.Singleton;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class ReleaseMigrationVerticle extends AbstractMigrationVerticle {

	private static Logger log = LoggerFactory.getLogger(ReleaseMigrationVerticle.class);

	private MessageConsumer<Object> releaseMigrationConsumer;

	protected NodeMigrationHandler nodeMigrationHandler;

	@Inject
	public ReleaseMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot, NodeMigrationHandler nodeMigrationHandler) {
		super(db, boot);
		this.nodeMigrationHandler = nodeMigrationHandler;
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
			String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
			String releaseUuid = message.headers().get(UUID_HEADER);
			if (log.isDebugEnabled()) {
				log.debug("Release migration for release {" + releaseUuid + "} was requested");
			}

			Throwable failure = db.tx(() -> {
				try {
					Project project = boot.get().projectRoot().findByUuid(projectUuid);
					if (project == null) {
						throw error(BAD_REQUEST, "Project with uuid {" + projectUuid + "} cannot be found");
					}

					Release release = project.getReleaseRoot().findByUuid(releaseUuid);
					if (release == null) {
						throw error(BAD_REQUEST, "Release with uuid {" + releaseUuid + "} cannot be found");
					}

					nodeMigrationHandler.migrateNodes(release);
					return null;
				} catch (Throwable t) {
					return t;
				}
			});
			if (failure != null) {
				message.fail(0, failure.getLocalizedMessage());
			} else {
				JsonObject msg = new JsonObject();
				msg.put("type", "completed");
				// TODO maybe a different address should be used
				vertx.eventBus().publish(MESH_MIGRATION, msg);
				message.reply(null);
			}
		});
	}
}
