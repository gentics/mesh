package com.gentics.mesh.core.verticle.migration;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Lock;
import rx.functions.Action0;
import rx.functions.Action1;

public class AbstractMigrationVerticle extends AbstractVerticle {

	private static final int MAX_MIGRATION_DATE_ENTRIES = 20;

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationVerticle.class);

	private static final String GLOBAL_MIGRATION_LOCK_NAME = "mesh.global.migrationlock";

	protected MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	public final static String PROJECT_UUID_HEADER = "projectUuid";

	public final static String RELEASE_UUID_HEADER = "releaseUuid";

	public final static String UUID_HEADER = "uuid";

	public static final String FROM_VERSION_UUID_HEADER = "fromVersion";

	public static final String TO_VERSION_UUID_HEADER = "toVersion";

	private static final String MIGRATION_DATA_MAP_KEY = "mesh.migration.data";

	protected Database db;

	protected Lazy<BootstrapInitializer> boot;

	public AbstractMigrationVerticle(Database db, Lazy<BootstrapInitializer> boot) {
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

	// protected synchronized boolean isRunning(ObjectName statusMBeanName) {
	// LocalMap<Object, Object> map = vertx.sharedData().getLocalMap("migrationStatus");
	// boolean isRunningOnCluster = map != null && map.get("status") != null && map.get("status").equals("migration_status_running");
	// return mbs.isRegistered(statusMBeanName) || isRunningOnCluster;
	// }

	// protected synchronized void setRunning(NodeMigrationStatus statusBean, ObjectName statusMBeanName) {
	// try {
	// mbs.registerMBean(statusBean, statusMBeanName);
	// } catch (Exception e1) {
	// }
	// JsonObject msg = new JsonObject();
	// msg.put("type", "started");
	// vertx.eventBus().publish(MESH_MIGRATION.toString(), msg);
	// vertx.sharedData().getLocalMap("migrationStatus").put("status", "migration_status_running");
	// }

	// /**
	// * Set the status of the migration in various places.
	// * <hr>
	// * This method will:
	// * <ul>
	// * <li>Publish an event which contains the status completed for migrations</li>
	// * <li>Set the migration status within the shared data map</li>
	// * <li>Unregister the migration status bean</li>
	// * </ul>
	// *
	// * @param schemaUuid
	// * @param statusMBeanName
	// */
	// protected synchronized void setDone(Message<Object> message, String schemaUuid, ObjectName statusMBeanName) {
	// if (log.isDebugEnabled()) {
	// log.debug("Migration for container " + schemaUuid + " completed");
	// }
	// try {
	// mbs.unregisterMBean(statusMBeanName);
	// } catch (Exception e1) {
	// }
	// JsonObject msg = new JsonObject();
	// msg.put("type", "completed");
	// Mesh.vertx().sharedData().getLocalMap("migrationStatus").put("status", "migration_status_idle");
	// Mesh.vertx().eventBus().publish(MESH_MIGRATION.toString(), msg);
	// message.reply(null);
	// }
	//
	// protected synchronized void setError(Message<Object> message, String schemaUuid, ObjectName statusMBeanName, Exception e) {
	// log.error("Migration for schema/microschema {" + schemaUuid + "} failed with error.", e);
	// message.fail(0, "Migration for schema/microschema {" + schemaUuid + "} failed: " + e.getLocalizedMessage());
	// try {
	// mbs.unregisterMBean(statusMBeanName);
	// } catch (Exception e1) {
	// }
	// Mesh.vertx().sharedData().getLocalMap("migrationStatus").put("status", "migration_status_failed");
	// /*
	// * JsonObject msg = new JsonObject(); msg.put("type", "completed"); Mesh.vertx().sharedData().getLocalMap("migrationStatus").put("status",
	// * "migration_status_idle"); Mesh.vertx().eventBus().publish(MESH_MIGRATION.toString(), msg);
	// */
	// }

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the success</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 * 
	 * @param message
	 */
	protected void done(Message<Object> message) {
		log.info("Migration completed without errors.");
		JsonObject result = new JsonObject().put("type", "completed");
		message.reply(result);
		vertx.eventBus().publish(MESH_MIGRATION, result);
		
		JsonObject info = new JsonObject();
		info.put("nodeId", Mesh.mesh().getOptions().getNodeName());
//		Schema/Microschema Uuid
//		Start version
//		Target version
//		Start time
//		Total amount of data which needs to be migrated
//		Counter which returns the amount of data which has already been migrated

		updateStatus(info);
	}

	public void updateStatus(JsonObject info) {
		if (Mesh.mesh().getOptions().getClusterOptions().isEnabled()) {
			vertx.sharedData().getLock("mesh.data.lock", rhl -> {
				if (rhl.failed()) {
					log.warn("Could not update status since lock could not be acquired", rhl.cause());
				} else {
					// Get the cluster data map
					vertx.sharedData().getClusterWideMap(MIGRATION_DATA_MAP_KEY, rh -> {
						if (rh.failed()) {
							log.error("Could not load data map", rh.cause());
						} else {
							// Get the json object from the map
							AsyncMap<Object, Object> map = rh.result();
							map.get("data", rd -> {
								if (rd.succeeded()) {
									JsonObject obj = (JsonObject) rd.result();
									if (obj == null) {
										obj = new JsonObject();
									}
									if (obj.containsKey("i")) {
										System.out.println(obj.getString("i"));
									}
									obj.put("i", "" + System.currentTimeMillis());
									obj.put("e", info);
									map.put("data", obj, ph -> {
										if (ph.failed()) {
											log.error("Could not store updated entry in map.", ph.cause());
										}
										rhl.result().release();
									});
								} else {
									log.error("Could not load data", rd.cause());
								}
							});
							purgeOldInfos(map);
						}
					});
				}
			});
		} else {
			LocalMap<Object, Object> map = vertx.sharedData().getLocalMap(MIGRATION_DATA_MAP_KEY);
			purgeOldInfos(map);
		}
	}

	private void purgeOldInfos(LocalMap<Object, Object> map) {
		Set<Long> bogusKeys = new HashSet<>();
		// Sort the entries by the initial timestamp, skip the amount of
		// entries we want to keep and add the rest to the bogus keys list
		map.entrySet().stream().sorted((o1, o2) -> {
			Long key1 = (Long) o1.getKey();
			Long key2 = (Long) o2.getKey();
			return Long.compare(key1, key2);
		}).skip(MAX_MIGRATION_DATE_ENTRIES).map(e -> {
			bogusKeys.add((Long) e.getKey());
			return null;
		});
		// Finally remove the bogus keys
		for (Long key : bogusKeys) {
			map.remove(key);
		}

	}

	private void purgeOldInfos(AsyncMap<Object, Object> map) {
		System.out.println(map.getClass().getName());
		// MAX_MIGRATION_DATE_ENTRIES
		// map.clear(resultHandler);

	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the error</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 * 
	 * @param message
	 * @param error
	 * @param failureMessage
	 */
	protected void handleError(Message<Object> message, Throwable error, String failureMessage) {
		log.error("Error handling migration", error);
		message.fail(100, failureMessage);
		vertx.eventBus().publish(MESH_MIGRATION, new JsonObject().put("type", "failed"));

	}

}
