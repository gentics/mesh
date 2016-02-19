package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationStatus.Type;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

	public final static String UUID_HEADER = "uuid";

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting verticle" + getClass().getName());
		}

		vertx.eventBus().consumer(SCHEMA_MIGRATION_ADDRESS, (message) -> {

			String schemaUuid = message.headers().get(UUID_HEADER);
			if (log.isDebugEnabled()) {
				log.debug("Node migration for schema " + schemaUuid + " was requested");
			}

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + schemaUuid);
				// TODO when mesh is running in a cluster, this check is not enough, since JMX beans are bound to the JVM
				if (mbs.isRegistered(statusMBeanName)) {
					message.fail(0, "Migration for schema " + schemaUuid + " is already running");
				} else {
					setRunning();
					db.noTrx(() -> boot.schemaContainerRoot().findByUuid(schemaUuid)).subscribe(schemaContainer -> {
						NodeMigrationStatus statusBean = db.noTrx(() -> {
							return new NodeMigrationStatus(schemaContainer.getName(), schemaContainer.getVersion(), Type.schema);
						});
						try {
							mbs.registerMBean(statusBean, statusMBeanName);
						} catch (Exception e1) {
						}
						nodeMigrationHandler.migrateNodes(schemaContainer, statusBean);
					} , (e) -> message.fail(0, e.getLocalizedMessage()), () -> {
						try {
							mbs.unregisterMBean(statusMBeanName);
						} catch (Exception e1) {
						}
						message.reply(null);
					});
				}

				setDone(schemaUuid);

			} catch (Exception e2) {
				message.fail(0, "Migration for schema " + schemaUuid + " failed: " + e2.getLocalizedMessage());
			}
		});

		vertx.eventBus().consumer(MICROSCHEMA_MIGRATION_ADDRESS, (message) -> {

			String microschemaUuid = message.headers().get(UUID_HEADER);
			if (log.isDebugEnabled()) {
				log.debug("Micronode Migration for microschema " + microschemaUuid + " was requested");
			}

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + microschemaUuid);
				// TODO when mesh is running in a cluster, this check is not enough, since JMX beans are bound to the JVM
				if (mbs.isRegistered(statusMBeanName)) {
					message.fail(0, "Migration for microschema " + microschemaUuid + " is already running");
				} else {
					setRunning();
					db.noTrx(() -> boot.microschemaContainerRoot().findByUuid(microschemaUuid)).subscribe(microschemaContainer -> {
						NodeMigrationStatus statusBean = db.noTrx(() -> {
							return new NodeMigrationStatus(microschemaContainer.getName(), microschemaContainer.getVersion(), Type.microschema);
						});
						try {
							mbs.registerMBean(statusBean, statusMBeanName);
						} catch (Exception e1) {
						}
						nodeMigrationHandler.migrateMicronodes(microschemaContainer, statusBean);
					} , (e) -> message.fail(0, e.getLocalizedMessage()), () -> {
						try {
							mbs.unregisterMBean(statusMBeanName);
						} catch (Exception e1) {
						}
						message.reply(null);
					});
					setDone(microschemaUuid);
				}
			} catch (Exception e2) {
				message.fail(0, "Migration for microschema " + microschemaUuid + " failed: " + e2.getLocalizedMessage());
			}
		});
	}

	private void setRunning() {
		JsonObject msg = new JsonObject();
		msg.put("type", "started");
		vertx.eventBus().publish(MESH_MIGRATION.toString(), msg);
		vertx.sharedData().getLocalMap("migrationStatus").put("status", "migration_status_running");
	}

	private void setDone(String schemaUuid) {
		if (log.isDebugEnabled()) {
			log.debug("Migration for container " + schemaUuid + " completed");
		}
		JsonObject msg = new JsonObject();
		msg.put("type", "completed");
		vertx.eventBus().publish(MESH_MIGRATION.toString(), msg);
		vertx.sharedData().getLocalMap("migrationStatus").put("status", "migration_status_idle");
	}
}
