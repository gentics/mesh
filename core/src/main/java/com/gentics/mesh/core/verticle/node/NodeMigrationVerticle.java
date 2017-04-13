package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.lang.management.ManagementFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.verticle.node.NodeMigrationStatus.Type;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import dagger.Lazy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dedicated worker verticle which will handle schema and microschema migrations.
 */
@Singleton
public class NodeMigrationVerticle extends AbstractVerticle {

	public final static String JMX_MBEAN_NAME = "com.gentics.mesh:type=NodeMigration";

	private static Logger log = LoggerFactory.getLogger(NodeMigrationVerticle.class);

	protected NodeMigrationHandler nodeMigrationHandler;

	private Database db;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public NodeMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot, NodeMigrationHandler handler) {
		this.db = db;
		this.boot = boot;
		this.nodeMigrationHandler = handler;
	}

	public final static String SCHEMA_MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrateSchema";

	public final static String MICROSCHEMA_MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrateMicroschema";

	public final static String RELEASE_MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrateRelease";

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	private MessageConsumer<Object> schemaMigrationConsumer;
	private MessageConsumer<Object> releaseMigrationConsumer;
	private MessageConsumer<Object> microschemaMigrationConsumer;

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		registerSchemaMigration();
		registerMicroschemaMigration();
		registerReleaseMigration();
	}

	@Override
	public void stop() throws Exception {
		if (schemaMigrationConsumer != null) {
			schemaMigrationConsumer.unregister();
		}
		if (releaseMigrationConsumer != null) {
			releaseMigrationConsumer.unregister();
		}
		if (microschemaMigrationConsumer != null) {
			microschemaMigrationConsumer.unregister();
		}
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
				if (isRunning(statusMBeanName)) {
					fail(message, "Migration for schema {" + schemaUuid + "} is already running");
					return;
				} else {
					db.noTx(() -> {
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
								Type.schema);
						setRunning(statusBean, statusMBeanName);

						// Invoke the migration using the located elements
						nodeMigrationHandler.migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusBean).await();
						return null;
					});
					setDone(schemaUuid, statusMBeanName);
					message.reply(null);
				}
			} catch (Exception e) {
				message.fail(0, "Migration for schema {" + schemaUuid + "} failed: " + e.getLocalizedMessage());
			}
		});

	}

	/**
	 * Fail with specified message.
	 * 
	 * @param message
	 * @param msg
	 */
	private void fail(Message<Object> message, String msg) {
		message.fail(0, msg);
	}

	/**
	 * Register handler for microschema migration events.
	 */
	private void registerMicroschemaMigration() {
		microschemaMigrationConsumer = vertx.eventBus().consumer(MICROSCHEMA_MIGRATION_ADDRESS, (message) -> {

			String microschemaUuid = message.headers().get(UUID_HEADER);
			String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
			String releaseUuid = message.headers().get(RELEASE_UUID_HEADER);
			String fromVersionUuid = message.headers().get(FROM_VERSION_UUID_HEADER);
			String toVersionUuuid = message.headers().get(TO_VERSION_UUID_HEADER);

			if (log.isDebugEnabled()) {
				log.debug("Micronode migration for microschema {" + microschemaUuid + "} from version {" + fromVersionUuid + "} to version {"
						+ toVersionUuuid + "} was requested");
			}

			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + microschemaUuid);
				if (isRunning(statusMBeanName)) {
					message.fail(0, "Migration for microschema " + microschemaUuid + " is already running");
				} else {
					db.noTx(() -> {
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

						NodeMigrationStatus statusBean = new NodeMigrationStatus(schemaContainer.getName(), fromContainerVersion.getVersion(),
								Type.microschema);
						setRunning(statusBean, statusMBeanName);
						nodeMigrationHandler.migrateMicronodes(project, release, fromContainerVersion, toContainerVersion, statusBean).await();
						return null;
					});
					setDone(microschemaUuid, statusMBeanName);
					message.reply(null);
				}
			} catch (Exception e) {
				message.fail(0, "Migration for microschema " + microschemaUuid + " failed: " + e.getLocalizedMessage());
			}
		});
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

			Throwable error = db.noTx(() -> {
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
			if (error != null) {
				message.fail(0, error.getLocalizedMessage());
			} else {
				JsonObject msg = new JsonObject();
				msg.put("type", "completed");
				// TODO maybe a different address should be used
				vertx.eventBus().publish(MESH_MIGRATION.toString(), msg);
				message.reply(null);
			}
		});
	}

	private boolean isRunning(ObjectName statusMBeanName) {
		// TODO when mesh is running in a cluster, this check is not enough, since JMX beans are bound to the JVM
		return mbs.isRegistered(statusMBeanName);
	}

	private void setRunning(NodeMigrationStatus statusBean, ObjectName statusMBeanName) {
		try {
			mbs.registerMBean(statusBean, statusMBeanName);
		} catch (Exception e1) {
		}
		JsonObject msg = new JsonObject();
		msg.put("type", "started");
		vertx.eventBus().publish(MESH_MIGRATION.toString(), msg);
		vertx.sharedData().getLocalMap("migrationStatus").put("status", "migration_status_running");
	}

	/**
	 * Set the status of the migration in various places.
	 * <hr>
	 * This method will:
	 * <ul>
	 * <li>Publish an event which contains the status completed for migrations</li>
	 * <li>Set the migration status within the shared data map</li>
	 * <li>Unregister the migration status bean</li>
	 * </ul>
	 * 
	 * @param schemaUuid
	 * @param statusMBeanName
	 */
	private void setDone(String schemaUuid, ObjectName statusMBeanName) {
		if (log.isDebugEnabled()) {
			log.debug("Migration for container " + schemaUuid + " completed");
		}
		try {
			mbs.unregisterMBean(statusMBeanName);
		} catch (Exception e1) {
		}
		JsonObject msg = new JsonObject();
		msg.put("type", "completed");
		Mesh.vertx().sharedData().getLocalMap("migrationStatus").put("status", "migration_status_idle");
		Mesh.vertx().eventBus().publish(MESH_MIGRATION.toString(), msg);
	}
}
