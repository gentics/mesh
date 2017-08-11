package com.gentics.mesh.core.verticle.migration.node;

import static com.gentics.mesh.Events.SCHEMA_MIGRATION_ADDRESS;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.ObjectName;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationVerticle;
import com.gentics.mesh.core.verticle.migration.MigrationType;
import com.gentics.mesh.core.verticle.migration.NodeMigrationStatus;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dedicated worker verticle which will handle schema migrations.
 */
@Singleton
public class NodeMigrationVerticle extends AbstractMigrationVerticle {

	private static Logger log = LoggerFactory.getLogger(NodeMigrationVerticle.class);

	public final static String JMX_MBEAN_NAME = "com.gentics.mesh:type=NodeMigration";

	protected NodeMigrationHandler nodeMigrationHandler;

	@Inject
	public NodeMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot, NodeMigrationHandler handler) {
		super(db, boot);
		this.nodeMigrationHandler = handler;
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

			String schemaUuid = message.headers().get(UUID_HEADER);
			String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
			String releaseUuid = message.headers().get(RELEASE_UUID_HEADER);
			String fromVersionUuid = message.headers().get(FROM_VERSION_UUID_HEADER);
			String toVersionUuid = message.headers().get(TO_VERSION_UUID_HEADER);

			if (log.isDebugEnabled()) {
				log.debug("Node migration for schema {" + schemaUuid + "} from version {" + fromVersionUuid + "} to version {" + toVersionUuid
						+ "} for release {" + releaseUuid + "} in project {" + projectUuid + "} was requested");
			}

			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + schemaUuid);
				db.tx(() -> {
					try {
						// Load the identified elements
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
						NodeMigrationStatus statusBean = new NodeMigrationStatus(schemaContainer.getName(), fromContainerVersion.getVersion(),
								MigrationType.schema);

						if (checkAndLock(statusMBeanName, statusBean)) {
							fail(message, "Migration for schema {" + schemaUuid + "} is already running");
							return null;
						}
						// Invoke the migration using the located elements
						nodeMigrationHandler.migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusBean).await();
					} catch (Exception e) {
						setError(message, schemaUuid, statusMBeanName, e);
					}
					return null;
				});
				setDone(message, schemaUuid, statusMBeanName);
			} catch (Exception e) {
				log.error("Error while generation jmx bean name", e);
			}
		});

	}

}
