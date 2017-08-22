package com.gentics.mesh.core.verticle.migration.node;

import static com.gentics.mesh.Events.SCHEMA_MIGRATION_ADDRESS;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.admin.MigrationType;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationVerticle;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dedicated worker verticle which will handle schema migrations.
 */
@Singleton
public class NodeMigrationVerticle extends AbstractMigrationVerticle<NodeMigrationHandler> {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationVerticle.class);

	@Inject
	public NodeMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot, NodeMigrationHandler handler) {
		super(handler, db, boot);
	}

	private MessageConsumer<Object> schemaMigrationConsumer;

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		registerSchemaMigration();
		super.start();
	}

	@Override
	public void stop() throws Exception {
		if (schemaMigrationConsumer != null) {
			schemaMigrationConsumer.unregister();
		}
		super.stop();
	}

	/**
	 * Register an event bus consumer handler which will react on schema node migration events.
	 */
	private void registerSchemaMigration() {
		schemaMigrationConsumer = Mesh.vertx().eventBus().consumer(SCHEMA_MIGRATION_ADDRESS, (message) -> {

			MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(message, vertx, MigrationType.schema);
			try {
				String schemaUuid = message.headers().get(UUID_HEADER);
				Objects.requireNonNull(schemaUuid, "The schemaUuid was not set the header.");
				String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
				Objects.requireNonNull(projectUuid, "The project uuid was not set the header.");
				String releaseUuid = message.headers().get(RELEASE_UUID_HEADER);
				Objects.requireNonNull(releaseUuid, "The release uuid was not set in the header.");
				String fromVersionUuid = message.headers().get(FROM_VERSION_UUID_HEADER);
				Objects.requireNonNull(fromVersionUuid, "The fromVersionUuid was not set in the header.");
				String toVersionUuid = message.headers().get(TO_VERSION_UUID_HEADER);
				Objects.requireNonNull(toVersionUuid, "The toVersionUuid was not set in the header.");

				if (log.isDebugEnabled()) {
					log.debug("Node migration for schema {" + schemaUuid + "} from version {" + fromVersionUuid + "} to version {" + toVersionUuid
							+ "} for release {" + releaseUuid + "} in project {" + projectUuid + "} was requested.");
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
					SchemaContainer schemaContainer = boot.get().schemaContainerRoot().findByUuid(schemaUuid);
					if (schemaContainer == null) {
						throw error(BAD_REQUEST, "Schema container for uuid {" + schemaUuid + "} can't be found.");
					}
					SchemaContainerVersion fromContainerVersion = schemaContainer.findVersionByUuid(fromVersionUuid);
					if (fromContainerVersion == null) {
						throw error(BAD_REQUEST, "Source version {" + fromVersionUuid + "} of schema {" + schemaUuid + "} could not be found.");
					}
					SchemaContainerVersion toContainerVersion = schemaContainer.findVersionByUuid(toVersionUuid);
					if (toContainerVersion == null) {
						throw error(BAD_REQUEST, "Target version {" + toVersionUuid + "} of schema {" + schemaUuid + "} could not be found.");
					}

					statusHandler.getInfo().setSourceName(schemaContainer.getName());
					statusHandler.getInfo().setSourceUuid(schemaContainer.getUuid());
					statusHandler.getInfo().setSourceVersion(fromContainerVersion.getVersion());
					statusHandler.getInfo().setTargetVersion(toContainerVersion.getVersion());
					statusHandler.updateStatus();

					// Acquire the global lock and invoke the migration
					executeLocked(() -> {
						db.tx(() -> {
							handler.migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusHandler).await();
						});
						statusHandler.done();
					}, (error) -> {
						statusHandler.error(error, "Migration for schema {" + schemaUuid + "} is already running.");
					});
				});
			} catch (Exception e) {
				statusHandler.error(e, "Error while preparing node migration.");
			}
		});

	}

}
