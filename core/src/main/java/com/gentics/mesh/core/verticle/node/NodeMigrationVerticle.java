package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.verticle.node.NodeMigrationStatus.Type;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dedicated worker verticle which will handle schema and microschema migrations.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class NodeMigrationVerticle extends AbstractSpringVerticle {

	public final static String JMX_MBEAN_NAME = "com.gentics.mesh:type=NodeMigration";

	private static Logger log = LoggerFactory.getLogger(NodeMigrationVerticle.class);

	@Autowired
	protected NodeMigrationHandler nodeMigrationHandler;

	public final static String SCHEMA_MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrateSchema";

	public final static String MICROSCHEMA_MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrateMicroschema";

	public final static String RELEASE_MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrateRelease";

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}");
		}
		registerSchemaMigration();
		registerMicroschemaMigration();
		registerReleaseMigration();
	}

	private void registerSchemaMigration() {
		vertx.eventBus().consumer(SCHEMA_MIGRATION_ADDRESS, (message) -> {

			String schemaUuid = message.headers().get(UUID_HEADER);
			String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
			String releaseUuid = message.headers().get(RELEASE_UUID_HEADER);
			String fromVersionUuid = message.headers().get(FROM_VERSION_UUID_HEADER);
			String toVersionUuid = message.headers().get(TO_VERSION_UUID_HEADER);

			if (log.isDebugEnabled()) {
				log.debug("Node migration for schema {" + schemaUuid + "} from version {" + fromVersionUuid
						+ "} to version {" + toVersionUuid + "} for release {" + releaseUuid + "} in project {"
						+ projectUuid + "} was requested");
			}

			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + schemaUuid);
				if (isRunning(statusMBeanName)) {
					fail(message, "Migration for schema {" + schemaUuid + "} is already running");
					return;
				} else {
					db.noTrx(() -> {
						Project project = boot.projectRoot().findByUuidSync(projectUuid);
						if (project == null) {
							throw error(BAD_REQUEST, "Project for uuid {" + projectUuid + "} not found");
						}
						Release release = project.getReleaseRoot().findByUuidSync(releaseUuid);
						if (release == null) {
							throw error(BAD_REQUEST, "Release for uuid {" + releaseUuid + "} not found");
						}
						SchemaContainer schemaContainer = boot.schemaContainerRoot().findByUuid(schemaUuid).toBlocking().single();
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
						nodeMigrationHandler
								.migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusBean)
								.toBlocking().lastOrDefault(null);
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

	private void registerMicroschemaMigration() {
		vertx.eventBus().consumer(MICROSCHEMA_MIGRATION_ADDRESS, (message) -> {

			String microschemaUuid = message.headers().get(UUID_HEADER);
			String fromVersionUuid = message.headers().get(FROM_VERSION_UUID_HEADER);
			String toVersionUuuid = message.headers().get(TO_VERSION_UUID_HEADER);

			if (log.isDebugEnabled()) {
				log.debug("Micronode migration for microschema {" + microschemaUuid + "} from version {" + fromVersionUuid + "} to version {" + toVersionUuuid
						+ "} was requested");
			}

			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + microschemaUuid);
				if (isRunning(statusMBeanName)) {
					message.fail(0, "Migration for microschema " + microschemaUuid + " is already running");
				} else {
					db.noTrx(() -> {
						MicroschemaContainer schemaContainer = boot.microschemaContainerRoot().findByUuid(microschemaUuid).toBlocking().single();

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
						nodeMigrationHandler.migrateMicronodes(fromContainerVersion, toContainerVersion, statusBean);
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
	 * Register handler for release migration
	 */
	private void registerReleaseMigration() {
		vertx.eventBus().consumer(RELEASE_MIGRATION_ADDRESS, (message) -> {
			String projectUuid = message.headers().get(PROJECT_UUID_HEADER);
			String releaseUuid = message.headers().get(UUID_HEADER);
			if (log.isDebugEnabled()) {
				log.debug("Release migration for release {" + releaseUuid + "} was requested");
			}

			Throwable error = db.noTrx(() -> {
				try {
					Project project = boot.projectRoot().findByUuidSync(projectUuid);
					if (project == null) {
						throw error(BAD_REQUEST, "Project with uuid {" + projectUuid + "} cannot be found");
					}

					Release release = project.getReleaseRoot().findByUuidSync(releaseUuid);
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
		vertx.sharedData().getLocalMap("migrationStatus").put("status", "migration_status_idle");
		vertx.eventBus().publish(MESH_MIGRATION.toString(), msg);
	}
}
