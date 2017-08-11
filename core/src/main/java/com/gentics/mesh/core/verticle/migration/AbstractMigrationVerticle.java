package com.gentics.mesh.core.verticle.migration;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

public class AbstractMigrationVerticle extends AbstractVerticle {

	private static Logger log = LoggerFactory.getLogger(AbstractMigrationVerticle.class);

	protected MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	protected Database db;

	protected Lazy<BootstrapInitializer> boot;

	public AbstractMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot) {
		this.db = db;
		this.boot = boot;
	}

	/**
	 * Checks for running migration and locks the execution if no other process is currently running.
	 * 
	 * @param statusBean
	 */
	protected synchronized boolean checkAndLock(ObjectName statusMBeanName, NodeMigrationStatus statusBean) {
		boolean running = isRunning(statusMBeanName);
		if (running) {
			return true;
		} else {
			setRunning(statusBean, statusMBeanName);
			return false;
		}
	}

	protected synchronized boolean isRunning(ObjectName statusMBeanName) {
		LocalMap<Object, Object> map = vertx.sharedData().getLocalMap("migrationStatus");
		boolean isRunningOnCluster = map != null && map.get("status") != null && map.get("status").equals("migration_status_running");
		return mbs.isRegistered(statusMBeanName) || isRunningOnCluster;
	}

	protected synchronized void setRunning(NodeMigrationStatus statusBean, ObjectName statusMBeanName) {
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
	protected synchronized void setDone(Message<Object> message, String schemaUuid, ObjectName statusMBeanName) {
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
		message.reply(null);
	}

	protected synchronized void setError(Message<Object> message, String schemaUuid, ObjectName statusMBeanName, Exception e) {
		log.error("Migration for schema/microschema {" + schemaUuid + "} failed with error.", e);
		message.fail(0, "Migration for schema/microschema {" + schemaUuid + "} failed: " + e.getLocalizedMessage());
		try {
			mbs.unregisterMBean(statusMBeanName);
		} catch (Exception e1) {
		}
		Mesh.vertx().sharedData().getLocalMap("migrationStatus").put("status", "migration_status_failed");
		/*
		 * JsonObject msg = new JsonObject(); msg.put("type", "completed"); Mesh.vertx().sharedData().getLocalMap("migrationStatus").put("status",
		 * "migration_status_idle"); Mesh.vertx().eventBus().publish(MESH_MIGRATION.toString(), msg);
		 */
	}

	/**
	 * Fail with specified message.
	 * 
	 * @param message
	 * @param msg
	 */
	protected void fail(Message<Object> message, String msg) {
		message.fail(0, msg);
	}

}
