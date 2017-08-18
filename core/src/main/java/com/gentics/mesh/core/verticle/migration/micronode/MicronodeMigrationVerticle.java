package com.gentics.mesh.core.verticle.migration.micronode;

import static com.gentics.mesh.Events.MICROSCHEMA_MIGRATION_ADDRESS;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationVerticle;
import com.gentics.mesh.core.verticle.migration.MigrationStatus;
import com.gentics.mesh.core.verticle.migration.MigrationType;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusImpl;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dedicated worker verticle which will micronode migrations.
 */
@Singleton
public class MicronodeMigrationVerticle extends AbstractMigrationVerticle<MicronodeMigrationHandler> {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationVerticle.class);

	public final static String JMX_MBEAN_NAME = "com.gentics.mesh:type=MicronodeMigration";

	private MessageConsumer<Object> microschemaMigrationConsumer;

	@Inject
	public MicronodeMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot, MicronodeMigrationHandler micronodeMigrationHandler) {
		super(micronodeMigrationHandler, db, boot);
	}

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		registerMicroschemaMigration();
		super.start();
	}

	@Override
	public void stop() throws Exception {
		if (microschemaMigrationConsumer != null) {
			microschemaMigrationConsumer.unregister();
		}
		super.stop();
	}

	/**
	 * Register handler for microschema migration events.
	 */
	private void registerMicroschemaMigration() {
		microschemaMigrationConsumer = vertx.eventBus().consumer(MICROSCHEMA_MIGRATION_ADDRESS, (message) -> {
			MigrationStatus status = new MigrationStatusImpl(message, vertx, MigrationType.microschema);
			try {
				
				String microschemaUuid = message.headers().get(UUID_HEADER);
				String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
				String releaseUuid = message.headers().get(RELEASE_UUID_HEADER);
				String fromVersionUuid = message.headers().get(FROM_VERSION_UUID_HEADER);
				String toVersionUuuid = message.headers().get(TO_VERSION_UUID_HEADER);

				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + microschemaUuid + "} from version {" + fromVersionUuid + "} to version {"
							+ toVersionUuuid + "} was requested");
				}

				db.tx(() -> {
					Project project = boot.get().projectRoot().findByUuid(projectUuid);
					if (project == null) {
						throw error(BAD_REQUEST, "Project for uuid {" + projectUuid + "} not found");
					}
					Release release = project.getReleaseRoot().findByUuid(releaseUuid);
					if (release == null) {
						throw error(BAD_REQUEST, "Release for uuid {" + releaseUuid + "} not found");
					}

					MicroschemaContainer schemaContainer = boot.get().microschemaContainerRoot().findByUuid(microschemaUuid);

					if (schemaContainer == null) {
						throw error(BAD_REQUEST, "Microschema container for uuid {" + microschemaUuid + "} can't be found.");
					}
					MicroschemaContainerVersion fromContainerVersion = schemaContainer.findVersionByUuid(fromVersionUuid);
					if (fromContainerVersion == null) {
						throw error(BAD_REQUEST,
								"Source version uuid {" + fromVersionUuid + "} of microschema {" + microschemaUuid + "} could not be found.");
					}
					MicroschemaContainerVersion toContainerVersion = schemaContainer.findVersionByUuid(toVersionUuuid);
					if (toContainerVersion == null) {
						throw error(BAD_REQUEST,
								"Target version uuid {" + toVersionUuuid + "} of microschema {" + microschemaUuid + "} could not be found.");
					}

					// Acquire the global lock and invoke the migration
					executeLocked(() -> {
						db.tx(() -> {
							handler.migrateMicronodes(project, release, fromContainerVersion, toContainerVersion, status)
									.await();
						});
						status.done(message);
					}, (error) -> {
						status.handleError(message, error, "Migration for microschema {" + microschemaUuid + "} is already running.");
					});
				});
			} catch (Exception e) {
				status.handleError(message, e, "Error while preparing micronode migration.");
			}
		});

	}

}
